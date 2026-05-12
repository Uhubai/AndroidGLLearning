/**
 * Day 9: MVP 矩阵组合
 *
 * 本渲染器演示：
 * 1. MVP 三个矩阵的完整管线：Model → View → Projection
 * 2. 矩阵乘法顺序的重要性：P × V × M × v（从右到左执行）
 * 3. 视图矩阵（View Matrix）概念：相机位置和朝向
 * 4. 多物体渲染：两个矩形使用不同的 Model 矩阵
 *
 * 绘制内容：
 * - 矩形1：原地旋转（红色渐变）
 * - 矩形2：绕原点公转（蓝色渐变）
 *
 * MVP 矩阵职责：
 * - Model：物体自身变换（平移、旋转、缩放）
 * - View：相机位置和朝向
 * - Projection：正交投影，屏幕适配
 */
package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day9Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day9Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val COLORS_PER_VERTEX = 3   // 每个顶点有 3 个颜色分量 (r, g, b)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        private const val SHORT_SIZE = 2          // Short 类型占用 2 字节
        
        /**
         * 顶点着色器代码
         *
         * 接收 MVP 组合矩阵并处理顶点位置和颜色
         *
         * 变量说明：
         * - uniform mat4 u_Matrix: MVP 组合矩阵（Projection × View × Model）
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
         *   顶点0 (-50, 50) 红色 ──── 顶点2 (50, 50) 蓝色
         *   │                                  │
         *   │                                  │
         *   顶点1 (-50, -50) 绿色 ──── 顶点3 (50, -50) 黄色
         *
         * 坐标范围：-50 到 50，矩形中心在原点
         */
        private val RECT_COORDS_AND_COLORS = floatArrayOf(
            -50f,  50f,  1.0f, 0.0f, 0.0f,  // 顶点0: 左上，红色
            -50f, -50f,  0.0f, 1.0f, 0.0f,  // 顶点1: 左下，绿色
             50f,  50f,  0.0f, 0.0f, 1.0f,  // 顶点2: 右上，蓝色
             50f, -50f,  1.0f, 1.0f, 0.0f   // 顶点3: 右下，黄色
        )
        
        /**
         * 矩形顶点索引数据
         *
         * 两个三角形组成一个矩形：
         * - 三角形1: 顶点 0, 1, 2（左上、左下、右上）
         * - 三角形2: 顶点 1, 3, 2（左下、右下、右上）
         */
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性句柄
    private var colorHandle: Int = 0          // 顶点颜色属性句柄
    private var matrixHandle: Int = 0         // MVP 组合矩阵 uniform 句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和颜色缓冲区
    private var indexBuffer: ShortBuffer? = null    // 顶点索引缓冲区
    
    // MVP 矩阵相关（4×4 矩阵 = 16 个浮点数）
    private val modelMatrix = FloatArray(16)        // 模型矩阵：物体自身变换
    private val viewMatrix = FloatArray(16)         // 视图矩阵：相机位置
    private val projectionMatrix = FloatArray(16)   // 投影矩阵：正交投影
    private val resultMatrix = FloatArray(16)       // 结果矩阵：P × V × M
    private val tempMatrix = FloatArray(16)         // 临时矩阵：V × M
    
    // 动画相关
    private var startTime: Long = 0                 // 动画开始时间
    
    /**
     * Surface 创建时的初始化回调
     *
     * 执行时机：GLSurfaceView 创建或重建时调用一次
     * 主要任务：
     * 1. 设置清屏颜色
     * 2. 初始化视图矩阵（相机位置）
     * 3. 创建并编译着色器
     * 4. 创建着色器程序
     * 5. 获取属性和 uniform 句柄
     * 6. 创建顶点和索引缓冲区
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色为黑色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 记录动画开始时间
        startTime = System.currentTimeMillis()
        
        // 初始化视图矩阵（相机位置）
        // setLookAtM(矩阵, 偏移, 相机位置, 观察目标, 上方向)
        // - 相机在 (0, 0, 3)：屏幕外 3 个单位
        // - 看向 (0, 0, 0)：原点
        // - 上方向 (0, 1, 0)：Y 轴朝上
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 3f,     // eyeX, eyeY, eyeZ（相机位置）
            0f, 0f, 0f,     // centerX, centerY, centerZ（观察目标）
            0f, 1f, 0f      // upX, upY, upZ（上方向）
        )
        
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
        // 注意：near/far 范围必须包含相机到物体的距离
        // 相机在 Z=3，物体在 Z=0，所以 near/far 至少需要 [-5, 5]
        if (aspectRatio > 1f) {
            // 横屏模式：扩展左右范围
            Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio * 100f, aspectRatio * 100f,  // left, right（范围 0-200）
                -100f, 100f,                               // bottom, top
                -10f, 10f                                  // near, far（包含相机距离）
            )
        } else {
            // 竖屏模式：扩展上下范围
            Matrix.orthoM(
                projectionMatrix, 0,
                -100f, 100f,                               // left, right
                -100f / aspectRatio, 100f / aspectRatio,   // bottom, top（范围 0-200）
                -10f, 10f                                  // near, far（包含相机距离）
            )
        }
    }
    
    /**
     * 每帧绘制回调
     *
     * 执行时机：每秒调用约 60 次
     * 主要任务：
     * 1. 清除屏幕
     * 2. 计算动画时间
     * 3. 绘制矩形1：原地旋转（左侧）
     * 4. 绘制矩形2：绕原点公转（右侧）
     *
     * MVP 矩阵组合流程：
     * - 每个物体有自己的 Model 矩阵
     * - View 矩阵共享（相机位置不变）
     * - Projection 矩阵共享（屏幕适配）
     * - 最终：result = P × V × M
     */
    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 计算动画时间
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - startTime) / 1000f
        
        // 激活着色器程序
        GLES20.glUseProgram(program)
        
        // ==================== 绘制矩形1：原地旋转（左侧） ====================
        
        // 步骤1：创建 Model 矩阵（平移到左侧 + 旋转）
        // 注意：矩阵变换从右到左应用，所以先写平移，后写旋转
        Matrix.setIdentityM(modelMatrix, 0)
        
        // 先平移到左侧（这是矩形的位置）
        Matrix.translateM(modelMatrix, 0, -80f, 0f, 0f)
        
        // 再旋转（绕自身中心原地旋转）
        val angle1 = elapsedSeconds * 60f  // 每秒 60 度
        Matrix.rotateM(modelMatrix, 0, angle1, 0f, 0f, 1f)
        
        // 步骤2：计算 MVP 组合矩阵
        // temp = V × M
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        
        // result = P × temp = P × V × M
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        // 步骤3：传递矩阵并绘制
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        drawRectangle()
        
        // ==================== 绘制矩形2：绕原点公转（右侧） ====================
        
        // 步骤1：创建 Model 矩阵（旋转 + 平移到右侧）
        // 注意：矩阵变换从右到左应用，所以先写旋转，后写平移
        Matrix.setIdentityM(modelMatrix, 0)
        
        // 先旋转（绕原点公转）
        val angle2 = elapsedSeconds * 45f  // 每秒 45 度
        Matrix.rotateM(modelMatrix, 0, angle2, 0f, 0f, 1f)
        
        // 再平移到右侧（这是公转半径）
        Matrix.translateM(modelMatrix, 0, 80f, 0f, 0f)
        
        // 步骤2：计算 MVP 组合矩阵
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        // 步骤3：传递矩阵并绘制
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        drawRectangle()
    }
    
    /**
     * 绘制矩形
     *
     * 复用顶点数据和索引缓冲区
     * 在调用前需要设置好 MVP 矩阵
     */
    private fun drawRectangle() {
        // 设置顶点属性指针
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
        
        // 绘制矩形
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            RECT_INDICES.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        
        // 清理
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