/**
 * Day 4: 正交投影矩阵 - 屏幕适配
 *
 * 本渲染器演示：
 * 1. 正交投影矩阵（Orthographic Projection）：无透视的平行投影
 * 2. 屏幕适配：根据宽高比调整可视范围，保持图形比例
 * 3. 像素坐标到 NDC 的转换：使用更大的坐标范围
 * 4. uniform 变量：所有顶点共享的全局数据（如矩阵）
 *
 * 绘制内容：一个带顶点颜色渐变的矩形
 * - 使用像素级坐标（-100 到 100）而非 NDC（-1 到 1）
 * - 通过投影矩阵转换到 NDC
 * - 无论屏幕宽窄，矩形始终保持正确比例
 *
 * 正交投影特点：
 * - 无透视效果：物体大小不随距离变化
 * - 适合 2D 图形和 UI 渲染
 * - 定义可视范围的边界（左右上下远近）
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

class Day4Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day4Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val COLORS_PER_VERTEX = 3   // 每个顶点有 3 个颜色分量 (r, g, b)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        private const val SHORT_SIZE = 2          // Short 类型占用 2 字节
        private const val WORLD_HALF_SIZE = 100f  // 世界坐标系半边长：矩形坐标范围为 [-100, 100]
        
        /**
         * 顶点着色器代码
         *
         * 新增内容：uniform mat4 u_Matrix
         *
         * 变量类型说明：
         * - uniform: 所有顶点共享的全局数据（矩阵、时间等）
         * - attribute: 每个顶点不同的数据（位置、颜色等）
         * - varying: 从顶点传递到片段的插值数据
         *
         * 矩阵变换：
         * gl_Position = u_Matrix * a_Position
         * - 先乘矩阵，再输出位置
         * - 投影矩阵将世界坐标转换为 NDC
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
         * 与之前相同，接收插值颜色输出像素颜色
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
         * 坐标范围变化：使用像素级坐标而非 NDC
         * - Day 2/3: 使用 [-0.5, 0.5] 的 NDC 坐标
         * - Day 4: 使用 [-100, 100] 的世界坐标
         *
         * 顶点布局：
         *   顶点0 (-100, 100) 红色 ──── 顶点2 (100, 100) 蓝色
         *   │                                    │
         *   │                                    │
         *   顶点1 (-100, -100) 绿色 ──── 顶点3 (100, -100) 黄色
         *
         * 投影矩阵会将这些坐标转换为 [-1, 1] 的 NDC
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
         * 与 Day 3 相同，定义两个三角形的顶点索引
         */
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性句柄
    private var colorHandle: Int = 0          // 顶点颜色属性句柄
    private var matrixHandle: Int = 0         // 投影矩阵 uniform 句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和颜色缓冲区
    private var indexBuffer: ShortBuffer? = null    // 顶点索引缓冲区
    
    // 投影矩阵
    private val projectionMatrix = FloatArray(16)   // 4×4 矩阵（16 个浮点数）
    
    /**
     * Surface 创建时的初始化回调
     *
     * 主要任务：
     * 1. 编译着色器并创建程序
     * 2. 获取属性和 uniform 句柄
     * 3. 创建顶点和索引缓冲区
     *
     * 新增：获取矩阵 uniform 句柄
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        // 创建并链接着色器程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 获取属性和 uniform 句柄
        // glGetAttribLocation: 获取 attribute 变量位置
        // glGetUniformLocation: 获取 uniform 变量位置
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(program, "a_Color")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        
        // 创建顶点数据缓冲区
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_COLORS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(RECT_COORDS_AND_COLORS)
        vertexBuffer?.position(0)
        
        // 创建索引数据缓冲区
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    /**
     * Surface 尺寸变化时的回调
     *
     * 主要任务：
     * 1. 设置视口大小
     * 2. 创建正交投影矩阵（屏幕适配）
     *
     * 正交投影矩阵原理：
     * - 定义可视范围的世界坐标边界
     * - 将范围内的坐标映射到 NDC [-1, 1]
     * - 超出范围的坐标会被裁剪
     *
     * 屏幕适配策略：
     * - 保持图形比例不变形
     * - 固定一个轴的范围，扩展另一个轴
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口
        GLES20.glViewport(0, 0, width, height)
        
        // 计算宽高比
        val aspectRatio = width.toFloat() / height.toFloat()
        
        // 创建正交投影矩阵
        // orthoM(matrix, offset, left, right, bottom, top, near, far)
        //
        // 屏幕适配逻辑（关键点）：
        // - 顶点坐标使用的是世界坐标范围 [-100, 100]
        // - 投影边界必须和世界坐标使用同一量级，否则图形会被裁剪
        // - 横屏（aspectRatio > 1）：扩展左右范围
        // - 竖屏（aspectRatio < 1）：扩展上下范围
        
        if (aspectRatio > 1f) {
            // 橫屏模式：宽 > 高
            // 固定上下范围为 [-WORLD_HALF_SIZE, WORLD_HALF_SIZE]
            // 扩展左右范围以匹配宽高比
            // 例如：16:9 屏幕，左右范围为 [-177.8, 177.8]
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio * WORLD_HALF_SIZE, aspectRatio * WORLD_HALF_SIZE,  // 左右边界：±(宽高比 × 基准尺寸)
                -WORLD_HALF_SIZE, WORLD_HALF_SIZE,                               // 上下边界：固定 ±100
                -1f, 1f                     // 近远边界
            )
        } else {
            // 竖屏模式：高 > 宽
            // 固定左右范围为 [-WORLD_HALF_SIZE, WORLD_HALF_SIZE]
            // 扩展上下范围以匹配宽高比
            // 例如：9:16 屏幕，上下范围为 [-177.8, 177.8]
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -WORLD_HALF_SIZE, WORLD_HALF_SIZE,                                    // 左右边界：固定 ±100
                -WORLD_HALF_SIZE / aspectRatio, WORLD_HALF_SIZE / aspectRatio,        // 上下边界：±(100/宽高比)
                -1f, 1f                         // 近远边界
            )
        }
        
        // 结果：无论屏幕宽窄，可视范围的"短边"始终为 [-1, 1]
        // 矩形（坐标 ±100）在投影后大约占据屏幕中心区域
    }
    
    /**
     * 每帧绘制回调
     *
     * 主要任务：
     * 1. 设置投影矩阵 uniform
     * 2. 设置顶点属性
     * 3. 绘制矩形
     */
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        GLES20.glUseProgram(program)
        
        // 传递投影矩阵给着色器
        // glUniformMatrix4fv：传递 4×4 矩阵数据
        // 参数：句柄、矩阵数量、是否转置（OpenGL ES 必须为 false）、矩阵数据、偏移
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, projectionMatrix, 0)
        
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
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}