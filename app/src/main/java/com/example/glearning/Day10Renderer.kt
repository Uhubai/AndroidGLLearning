/**
 * Day 10: 纹理基础 - 程序化纹理贴图
 *
 * 本渲染器演示：
 * 1. UV 坐标系统：纹理坐标映射 [0, 1]
 * 2. 纹理加载流程：生成、绑定、设置参数、加载数据
 * 3. 程序化纹理生成：在代码中创建棋盘格图案
 * 4. 纹理采样：GL_LINEAR vs GL_NEAREST 对比
 *
 * 绘制内容：
 * - 左侧矩形：棋盘格纹理（线性过滤，平滑）
 * - 右侧矩形：棋盘格纹理（最近邻过滤，像素化）
 *
 * UV 坐标说明：
 * - 范围 [0, 1]
 * - 原点在左下角
 * - U 轴向右，V 轴向上（与图片坐标系相反）
 */
package com.example.glearning

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.opengl.GLUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day10Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day10Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val UV_PER_VERTEX = 2       // 每个顶点有 2 个 UV 分量 (u, v)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        private const val SHORT_SIZE = 2          // Short 类型占用 2 字节
        
        /**
         * 顶点着色器代码
         *
         * 接收 MVP 组合矩阵、顶点位置和 UV 坐标
         *
         * 变量说明：
         * - uniform mat4 u_Matrix: MVP 组合矩阵
         * - attribute vec4 a_Position: 顶点位置
         * - attribute vec2 a_TextureCoord: 纹理 UV 坐标
         * - varying vec2 v_TextureCoord: 传递给片段着色器的 UV 坐标
         */
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_Matrix;
            attribute vec4 a_Position;
            attribute vec2 a_TextureCoord;
            varying vec2 v_TextureCoord;
            
            void main() {
                gl_Position = u_Matrix * a_Position;
                v_TextureCoord = a_TextureCoord;
            }
        """
        
        /**
         * 片段着色器代码
         *
         * 使用纹理采样器根据 UV 坐标采样纹理
         *
         * 变量说明：
         * - uniform sampler2D u_Texture: 2D 纹理采样器
         * - varying vec2 v_TextureCoord: 从顶点着色器传递的 UV 坐标
         * - texture2D(): 根据 UV 坐标采样纹理颜色的函数
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 v_TextureCoord;
            uniform sampler2D u_Texture;
            
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TextureCoord);
            }
        """
        
        /**
         * 矩形顶点坐标和 UV 数据
         *
         * 数据结构：交错存储
         * [x, y, u, v] × 4 个顶点 = 16 个浮点数
         *
         * 顶点布局：
         *   顶点0 (-50, 50) UV(0,1) ──── 顶点2 (50, 50) UV(1,1)
         *   │                                        │
         *   │                                        │
         *   顶点1 (-50,-50) UV(0,0) ──── 顶点3 (50,-50) UV(1,0)
         *
         * UV 坐标解释：
         * - (0,0) = 纹理左下角
         * - (1,1) = 纹理右上角
         * - V 轴从下到上（与屏幕坐标相反）
         */
        private val RECT_COORDS_AND_UVS = floatArrayOf(
            -50f,  50f,  0.0f, 1.0f,  // 左上：UV(0, 1)
            -50f, -50f,  0.0f, 0.0f,  // 左下：UV(0, 0)
             50f,  50f,  1.0f, 1.0f,  // 右上：UV(1, 1)
             50f, -50f,  1.0f, 0.0f   // 右下：UV(1, 0)
        )
        
        /**
         * 矩形顶点索引数据
         */
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
        
        /**
         * 生成棋盘格纹理
         *
         * 创建 8×8 的棋盘格图案
         * 黑白相间的格子，用于清晰展示纹理映射效果
         *
         * @return 生成的 Bitmap 对象
         */
        private fun createCheckerboardTexture(): Bitmap {
            val size = 8  // 8×8 像素
            val colors = IntArray(size * size)
            
            for (y in 0 until size) {
                for (x in 0 until size) {
                    // 判断当前格子是黑还是白
                    val isWhite = (x / 2 + y / 2) % 2 == 0
                    colors[y * size + x] = if (isWhite) {
                        0xFFFFFFFF.toInt()  // 白色
                    } else {
                        0xFF444444.toInt()  // 深灰色
                    }
                }
            }
            
            return Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成渐变纹理
         *
         * 创建 64×64 的彩虹渐变图案
         * 用于展示纹理颜色插值效果
         *
         * @return 生成的 Bitmap 对象
         */
        private fun createGradientTexture(): Bitmap {
            val size = 64
            val colors = IntArray(size * size)
            
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val r = (x * 255 / size).toByte()
                    val g = (y * 255 / size).toByte()
                    val b = 128.toByte()
                    colors[y * size + x] = 0xFF000000.toInt() or
                        ((r.toInt() and 0xFF) shl 16) or
                        ((g.toInt() and 0xFF) shl 8) or
                        (b.toInt() and 0xFF)
                }
            }
            
            return Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        }
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性句柄
    private var textureCoordHandle: Int = 0   // 纹理 UV 坐标属性句柄
    private var matrixHandle: Int = 0         // MVP 组合矩阵 uniform 句柄
    private var textureHandle: Int = 0        // 纹理采样器 uniform 句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和 UV 缓冲区
    private var indexBuffer: ShortBuffer? = null    // 顶点索引缓冲区
    
    // MVP 矩阵相关
    private val modelMatrix = FloatArray(16)        // 模型矩阵
    private val viewMatrix = FloatArray(16)         // 视图矩阵
    private val projectionMatrix = FloatArray(16)   // 投影矩阵
    private val resultMatrix = FloatArray(16)       // 结果矩阵：P × V × M
    private val tempMatrix = FloatArray(16)         // 临时矩阵：V × M
    
    // 纹理相关
    private var textureId1: Int = 0  // 纹理1（棋盘格）
    private var textureId2: Int = 0  // 纹理2（渐变）
    
    // 动画相关
    private var startTime: Long = 0  // 动画开始时间
    
    /**
     * Surface 创建时的初始化回调
     *
     * 执行时机：GLSurfaceView 创建或重建时调用一次
     * 主要任务：
     * 1. 设置清屏颜色
     * 2. 初始化视图矩阵
     * 3. 创建并编译着色器
     * 4. 加载纹理
     * 5. 创建顶点缓冲区
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色为深灰色
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        
        // 记录动画开始时间
        startTime = System.currentTimeMillis()
        
        // 初始化视图矩阵
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 3f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        
        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        // 创建着色器程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 获取属性和 uniform 句柄
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        textureCoordHandle = GLES20.glGetAttribLocation(program, "a_TextureCoord")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")
        
        // 创建纹理
        textureId1 = createTexture(createCheckerboardTexture())
        textureId2 = loadGradientTexture()
        
        // 创建顶点数据缓冲区
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_UVS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(RECT_COORDS_AND_UVS)
        vertexBuffer?.position(0)
        
        // 创建索引数据缓冲区
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    /**
     * 创建 OpenGL 纹理
     *
     * @param bitmap 要加载为纹理的位图
     * @return 纹理 ID
     */
    private fun createTexture(bitmap: Bitmap): Int {
        val textureId = IntArray(1)
        
        // 步骤1：生成纹理 ID
        GLES20.glGenTextures(1, textureId, 0)
        
        // 步骤2：绑定纹理到 GL_TEXTURE_2D 目标
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        
        // 步骤3：设置纹理参数
        // 缩小时的过滤方式：GL_LINEAR（线性插值，平滑）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        
        // 放大时的过滤方式：GL_LINEAR（线性插值，平滑）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        
        // 水平方向环绕模式：GL_CLAMP_TO_EDGE（边缘像素延伸）
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        
        // 垂直方向环绕模式：GL_CLAMP_TO_EDGE
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        // 步骤4：加载 Bitmap 数据到 GPU 纹理
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        
        // 释放 Bitmap（数据已加载到 GPU，不再需要）
        bitmap.recycle()
        
        // 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        
        return textureId[0]
    }
    
    /**
     * 生成渐变纹理（程序化）
     */
    private fun loadGradientTexture(): Int {
        val bitmap = createGradientTexture()
        val textureId = IntArray(1)
        
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        
        return textureId[0]
    }
    
    /**
     * Surface 尺寸变化时的回调
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val aspectRatio = width.toFloat() / height.toFloat()
        
        if (aspectRatio > 1f) {
            Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio * 100f, aspectRatio * 100f,
                -100f, 100f,
                -1f, 1f
            )
        } else {
            Matrix.orthoM(
                projectionMatrix, 0,
                -100f, 100f,
                -100f / aspectRatio, 100f / aspectRatio,
                -1f, 1f
            )
        }
    }
    
    /**
     * 每帧绘制回调
     *
     * 绘制两个矩形对比纹理过滤效果：
     * - 左侧：棋盘格（GL_LINEAR 过滤）
     * - 右侧：渐变纹理（GL_NEAREST 过滤）
     */
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
        
        // 激活着色器
        GLES20.glUseProgram(program)
        
        // 设置纹理单元 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureHandle, 0)
        
        // ==================== 绘制左侧：棋盘格（线性过滤） ====================
        
        // 绑定纹理1
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId1)
        
        // 计算 MVP 矩阵（平移到左侧 + 轻微旋转）
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, -80f, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, elapsedSeconds * 20f, 0f, 0f, 1f)
        
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        drawRectangle()
        
        // ==================== 绘制右侧：渐变（最近邻过滤） ====================
        
        // 绑定纹理2
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId2)
        
        // 计算 MVP 矩阵（平移到右侧）
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 80f, 0f, 0f)
        
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        drawRectangle()
        
        // 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
    
    /**
     * 绘制矩形
     */
    private fun drawRectangle() {
        val stride = (COORDS_PER_VERTEX + UV_PER_VERTEX) * FLOAT_SIZE
        
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
        
        // 纹理 UV 坐标
        vertexBuffer?.position(COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(
            textureCoordHandle,
            UV_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            stride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        
        // 绘制
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            RECT_INDICES.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}