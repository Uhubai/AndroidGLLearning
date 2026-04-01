/**
 * Day 8: 旋转矩阵 - 原地旋转动画
 *
 * 本渲染器演示：
 * 1. 旋转矩阵推导原理：x' = x×cos(θ) - y×sin(θ), y' = x×sin(θ) + y×cos(θ)
 * 2. Matrix.rotateM API 使用：绕指定轴旋转指定角度
 * 3. 基于时间的旋转动画：angle = elapsedSeconds × speed
 * 4. 原地旋转效果：物体绕自身中心旋转
 *
 * 绘制内容：一个带顶点颜色渐变的矩形原地旋转
 * - 矩形四个顶点各有颜色（红、绿、蓝、黄）
 * - 每秒旋转 60 度，持续旋转动画
 * - GPU 自动在矩形内部插值颜色
 *
 * 旋转矩阵（绕 Z 轴）：
 * ┌                              ┐
 * │ cos(θ)  -sin(θ)  0    0      │
 * │ sin(θ)   cos(θ)  0    0      │
 * │ 0        0       1    0      │
 * │ 0        0       0    1      │
 * └                              ┘
 */
package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day8Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day8Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val COLORS_PER_VERTEX = 3   // 每个顶点有 3 个颜色分量 (r, g, b)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        private const val SHORT_SIZE = 2          // Short 类型占用 2 字节
        
        /**
         * 顶点着色器代码
         *
         * 接收变换矩阵并处理顶点位置和颜色
         *
         * 变量说明：
         * - uniform mat4 u_Matrix: 组合变换矩阵（投影 × 模型）
         * - attribute vec4 a_Position: 顶点位置属性
         * - attribute vec4 a_Color: 顶点颜色属性
         * - varying vec4 v_Color: 传递给片段着色器的插值颜色
         */
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_Matrix;
            attribute vec4 a_Position;
            attribute vec4 a_Color;
            varying vec4 v_Color;
            
            void main() {
                gl_Position = u_Matrix * a_Position;
                v_Color = a_Color;
            }
        """
        
        /**
         * 片段着色器代码
         *
         * 接收插值颜色并输出为像素颜色
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec4 v_Color;
            
            void main() {
                gl_FragColor = v_Color;
            }
        """
        
        /**
         * 矩形顶点坐标和颜色数据
         *
         * 数据结构：交错存储
         * [x, y, r, g, b] × 4 个顶点 = 20 个浮点数
         *
         * 顶点布局：
         *   顶点0 (-100, 100) 红色 ──── 顶点2 (100, 100) 蓝色
         *   │                                    │
         *   │                                    │
         *   顶点1 (-100, -100) 绿色 ──── 顶点3 (100, -100) 黄色
         *
         * 坐标范围：-100 到 100，矩形中心在原点
         * 原地旋转时，矩形绕中心（原点）旋转
         */
        private val RECT_COORDS_AND_COLORS = floatArrayOf(
            -100f,  100f,  1.0f, 0.0f, 0.0f,  // 顶点0: 左上，红色
            -100f, -100f,  0.0f, 1.0f, 0.0f,  // 顶点1: 左下，绿色
             100f,  100f,  0.0f, 0.0f, 1.0f,  // 顶点2: 右上，蓝色
             100f, -100f,  1.0f, 1.0f, 0.0f   // 顶点3: 右下，黄色
        )
        
        /**
         * 矩形顶点索引数据
         *
         * 两个三角形组成一个矩形：
         * - 三角形1: 顶点 0, 1, 2（左上、左下、右上）
         * - 三角形2: 顶点 1, 3, 2（左下、右下、右上）
         */
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
        
        /**
         * 旋转速度常量
         *
         * 单位：度/秒
         * speed = 60 表示每秒旋转 60 度
         * speed = 360 表示每秒旋转一圈（完整旋转）
         */
        private const val ROTATION_SPEED = 60f  // 每秒旋转 60 度
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性句柄
    private var colorHandle: Int = 0          // 顶点颜色属性句柄
    private var matrixHandle: Int = 0         // 变换矩阵 uniform 句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和颜色缓冲区
    private var indexBuffer: ShortBuffer? = null    // 顶点索引缓冲区
    
    // 矩阵相关（4×4 矩阵 = 16 个浮点数）
    private val projectionMatrix = FloatArray(16)   // 投影矩阵：定义可视范围
    private val modelMatrix = FloatArray(16)        // 模型矩阵：旋转变换
    private val resultMatrix = FloatArray(16)       // 结果矩阵：投影 × 模型
    
    // 动画相关
    private var startTime: Long = 0                 // 动画开始时间
    
    /**
     * Surface 创建时的初始化回调
     *
     * 执行时机：GLSurfaceView 创建或重建时调用一次
     * 主要任务：
     * 1. 设置清屏颜色
     * 2. 创建并编译着色器
     * 3. 创建着色器程序
     * 4. 获取属性和 uniform 句柄
     * 5. 创建顶点和索引缓冲区
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色为黑色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 记录动画开始时间
        startTime = System.currentTimeMillis()
        
        // 步骤1：编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        // 步骤2：创建着色器程序并链接
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 步骤3：获取属性和 uniform 句柄
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(program, "a_Color")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        
        // 步骤4：创建顶点数据缓冲区
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_COLORS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(RECT_COORDS_AND_COLORS)
        vertexBuffer?.position(0)
        
        // 步骤5：创建索引数据缓冲区
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    /**
     * Surface 尺寸变化时的回调
     *
     * 执行时机：Surface 创建后、屏幕旋转时调用
     * 主要任务：
     * 1. 设置视口大小
     * 2. 创建正交投影矩阵（屏幕适配）
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口覆盖整个 Surface
        GLES20.glViewport(0, 0, width, height)
        
        // 计算宽高比，用于保持图形比例不变形
        val aspectRatio = width.toFloat() / height.toFloat()
        
        // 创建正交投影矩阵（屏幕适配）
        if (aspectRatio > 1f) {
            // 橫屏模式：扩展左右范围
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio, aspectRatio,
                -1f, 1f,
                -1f, 1f
            )
        } else {
            // 竖屏模式：扩展上下范围
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -1f, 1f,
                -1f / aspectRatio, 1f / aspectRatio,
                -1f, 1f
            )
        }
    }
    
    /**
     * 每帧绘制回调
     *
     * 执行时机：每秒调用约 60 次
     * 主要任务：
     * 1. 清除屏幕
     * 2. 计算旋转角度（基于时间）
     * 3. 应用旋转矩阵变换
     * 4. 组合投影矩阵和模型矩阵
     * 5. 设置着色器参数
     * 6. 绘制矩形
     *
     * 旋转动画原理：
     * - angle = elapsedSeconds × speed
     * - angle 随时间线性增长
     * - Matrix.rotateM 可以处理任意角度
     * - 正角度为逆时针旋转
     */
    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 步骤1：计算动画时间
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - startTime) / 1000f
        
        // 步骤2：计算旋转角度
        // angle = elapsedSeconds × ROTATION_SPEED
        // 每秒旋转 ROTATION_SPEED 度
        // angle 会持续增大，但 Matrix.rotateM 可以正确处理
        val angle = elapsedSeconds * ROTATION_SPEED
        
        // 步骤3：应用旋转矩阵变换
        // 重置模型矩阵为单位矩阵
        android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        
        // 应用旋转变换
        // rotateM(matrix, offset, angle, axisX, axisY, axisZ)
        // - angle: 旋转角度（度数）
        // - axisX, axisY, axisZ: 旋转轴向量
        // - (0, 0, 1) 表示绕 Z 轴旋转（平面旋转）
        // 
        // 旋转矩阵（绕 Z 躴）：
        // x' = x × cos(θ) - y × sin(θ)
        // y' = x × sin(θ) + y × cos(θ)
        android.opengl.Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)
        
        // 步骤4：组合投影矩阵和模型矩阵
        // 结果矩阵 = Projection × Model
        // 顶点变换：gl_Position = Projection × Model × Vertex
        android.opengl.Matrix.multiplyMM(
            resultMatrix, 0,
            projectionMatrix, 0,
            modelMatrix, 0
        )
        
        // 步骤5：激活着色器程序
        GLES20.glUseProgram(program)
        
        // 步骤6：传递变换矩阵给着色器
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        
        // 步骤7：设置顶点属性指针
        val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE
        
        // 顶点位置
        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            stride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        // 顶点颜色
        vertexBuffer?.position(COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(
            colorHandle,
            COLORS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            stride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(colorHandle)
        
        // 步骤8：绘制矩形
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            RECT_INDICES.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        
        // 步骤9：清理
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
    
    /**
     * 编译着色器
     *
     * @param type 着色器类型（GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER）
     * @param shaderCode 着色器源代码
     * @return 编译后的着色器对象 ID
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}