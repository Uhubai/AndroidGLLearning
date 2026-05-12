/**
 * Day 13: 复习 - 多物体变换与纹理综合练习
 *
 * 本渲染器用于复习前 12 天知识：
 * 1. 多个物体使用不同的 MVP 变换
 * 2. 每个物体绑定不同的纹理
 * 3. 同时进行多种动画效果
 *
 * 绘制内容：圆形排列的 6 个矩形，每个有不同的变换和纹理
 * - 矩形 1-2：平移动画 + 条纹纹理
 * - 矩形 3-4：旋转动画 + 漩涡纹理
 * - 矩形 5-6：缩放动画 + 渐变纹理
 *
 * 复习要点：
 * - MVP 矩阵组合流程
 * - 纹理加载和绑定
 * - 基于时间的动画计算
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
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.PI

class Day13Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day13Renderer"
        
        private const val COORDS_PER_VERTEX = 2
        private const val UV_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4
        private const val SHORT_SIZE = 2
        
        /**
         * 顶点着色器：MVP 矩阵 + 纹理坐标
         *
         * 复习：
         * - uniform mat4 u_Matrix: MVP 组合矩阵
         * - attribute vec4 a_Position: 顶点位置
         * - attribute vec2 a_TextureCoord: UV 坐标
         * - varying vec2 v_TextureCoord: 传递给片段着色器
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
         * 片段着色器：纹理采样
         *
         * 复习：
         * - uniform sampler2D u_Texture: 2D 纹理采样器
         * - texture2D(): 根据 UV 坐标采样
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
         * 复习：交错存储格式 [x, y, u, v]
         */
        private val RECT_COORDS_AND_UVS = floatArrayOf(
            -30f,  30f,  0.0f, 1.0f,
            -30f, -30f,  0.0f, 0.0f,
             30f,  30f,  1.0f, 1.0f,
             30f, -30f,  1.0f, 0.0f
        )
        
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
        
        /**
         * 生成条纹纹理
         */
        private fun createStripeTexture(): Bitmap {
            val width = 64
            val height = 64
            val colors = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val isStripe = (y / 8) % 2 == 0
                    colors[y * width + x] = if (isStripe) 0xFF4488FF.toInt() else 0xFF2244AA.toInt()
                }
            }
            return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成漩涡纹理
         */
        private fun createSwirlTexture(): Bitmap {
            val size = 64
            val colors = IntArray(size * size)
            val centerX = size / 2f
            val centerY = size / 2f
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val angle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / (size / 2f)
                    val r = ((sin(angle + dist * 10) * 0.5 + 0.5) * 255).toInt()
                    val g = ((cos(angle + dist * 8) * 0.5 + 0.5) * 255).toInt()
                    colors[y * size + x] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or 128
                }
            }
            return Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成渐变纹理
         */
        private fun createGradientTexture(): Bitmap {
            val size = 64
            val colors = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val r = (x * 255 / size).toByte()
                    val g = (y * 255 / size).toByte()
                    colors[y * size + x] = 0xFF000000.toInt() or ((r.toInt() and 0xFF) shl 16) or ((g.toInt() and 0xFF) shl 8) or 128
                }
            }
            return Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        }
    }
    
    // 着色器相关
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var textureCoordHandle: Int = 0
    private var matrixHandle: Int = 0
    private var textureHandle: Int = 0
    
    // 顶点缓冲区
    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    
    // MVP 矩阵
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val resultMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    
    // 纹理 ID
    private var textureId1: Int = 0  // 条纹
    private var textureId2: Int = 0  // 漩涡
    private var textureId3: Int = 0  // 渐变
    
    // 动画
    private var startTime: Long = 0
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        
        startTime = System.currentTimeMillis()
        
        // 初始化视图矩阵
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        
        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 获取句柄
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        textureCoordHandle = GLES20.glGetAttribLocation(program, "a_TextureCoord")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")
        
        // 创建纹理
        textureId1 = createTexture(createStripeTexture())
        textureId2 = createTexture(createSwirlTexture())
        textureId3 = createTexture(createGradientTexture())
        
        // 创建顶点缓冲区
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_UVS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(RECT_COORDS_AND_UVS)
        vertexBuffer?.position(0)
        
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    private fun createTexture(bitmap: Bitmap): Int {
        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        return textureId[0]
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val aspectRatio = width.toFloat() / height.toFloat()
        if (aspectRatio > 1f) {
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio * 150f, aspectRatio * 150f, -150f, 150f, 1f, 10f)
        } else {
            Matrix.orthoM(projectionMatrix, 0, -150f, 150f, -150f / aspectRatio, 150f / aspectRatio, 1f, 10f)
        }
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
        
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureHandle, 0)
        
        // 6 个矩形圆形排列，半径 80
        val radius = 80f
        for (i in 0 until 6) {
            val angle = (i * 60 + elapsedSeconds * 30) * (PI / 180f).toFloat()
            val tx = cos(angle) * radius
            val ty = sin(angle) * radius
            
            // 根据索引选择纹理和动画类型
            val textureId = when (i % 3) {
                0 -> textureId1
                1 -> textureId2
                else -> textureId3
            }
            
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            
            // 计算 MVP 矩阵
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
            
            // 不同索引应用不同变换
            when (i % 3) {
                0 -> {
                    // 平移动画（上下浮动）
                    val offsetY = sin(elapsedSeconds * 2f + i) * 20f
                    Matrix.translateM(modelMatrix, 0, 0f, offsetY, 0f)
                }
                1 -> {
                    // 旋转动画
                    Matrix.rotateM(modelMatrix, 0, elapsedSeconds * 60f + i * 60f, 0f, 0f, 1f)
                }
                2 -> {
                    // 缩放动画
                    val scale = 1.0f + sin(elapsedSeconds * 2f + i) * 0.3f
                    Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)
                }
            }
            
            Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
            Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
            
            GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
            drawRectangle()
        }
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
    
    private fun drawRectangle() {
        val stride = (COORDS_PER_VERTEX + UV_PER_VERTEX) * FLOAT_SIZE
        
        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        vertexBuffer?.position(COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(textureCoordHandle, UV_PER_VERTEX, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, RECT_INDICES.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}