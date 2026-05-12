/**
 * Day 11: 纹理变换 - UV 动画与纹理重复
 *
 * 本渲染器演示：
 * 1. 纹理变换矩阵：旋转、平移、缩放纹理
 * 2. 纹理重复模式：GL_REPEAT 实现纹理平铺
 * 3. UV 动画：通过变换 UV 坐标创建动态效果
 * 4. 多纹理混合：两层纹理叠加效果
 *
 * 绘制内容：
 * - 左上：纹理滚动动画（水流效果）
 * - 右上：纹理旋转动画（漩涡效果）
 * - 左下：纹理重复平铺（砖墙效果）
 * - 右下：纹理缩放动画（呼吸效果）
 *
 * 纹理变换原理：
 * - 与 MVP 矩阵类似，但作用于 UV 坐标
 * - 在顶点着色器或片段着色器中应用
 * - 可以创建各种动态纹理效果
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

class Day11Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day11Renderer"
        
        private const val COORDS_PER_VERTEX = 2
        private const val UV_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4
        private const val SHORT_SIZE = 2
        
        /**
         * 顶点着色器代码
         *
         * 新增：u_TextureMatrix 用于变换 UV 坐标
         *
         * 变换流程：
         * 1. 顶点位置：u_Matrix * a_Position
         * 2. 纹理坐标：u_TextureMatrix * a_TextureCoord
         */
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_Matrix;
            uniform mat4 u_TextureMatrix;
            attribute vec4 a_Position;
            attribute vec2 a_TextureCoord;
            varying vec2 v_TextureCoord;
            
            void main() {
                gl_Position = u_Matrix * a_Position;
                v_TextureCoord = (u_TextureMatrix * vec4(a_TextureCoord, 0.0, 1.0)).xy;
            }
        """
        
        /**
         * 片段着色器代码
         *
         * 使用变换后的 UV 坐标采样纹理
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
         * 标准 UV 范围 [0, 1]
         */
        private val RECT_COORDS_AND_UVS = floatArrayOf(
            -50f,  50f,  0.0f, 1.0f,
            -50f, -50f,  0.0f, 0.0f,
             50f,  50f,  1.0f, 1.0f,
             50f, -50f,  1.0f, 0.0f
        )
        
        /**
         * 重复纹理的 UV 数据（0 到 2，重复 2 次）
         */
        private val REPEAT_UVS = floatArrayOf(
            -50f,  50f,  0.0f, 2.0f,
            -50f, -50f,  0.0f, 0.0f,
             50f,  50f,  2.0f, 2.0f,
             50f, -50f,  2.0f, 0.0f
        )
        
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
        
        /**
         * 生成条纹纹理（用于演示纹理滚动）
         */
        private fun createStripeTexture(): Bitmap {
            val width = 64
            val height = 64
            val colors = IntArray(width * height)
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val isStripe = (y / 8) % 2 == 0
                    colors[y * width + x] = if (isStripe) {
                        0xFF4488FF.toInt()  // 蓝色条纹
                    } else {
                        0xFF2244AA.toInt()  // 深蓝条纹
                    }
                }
            }
            
            return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成漩涡纹理（用于演示纹理旋转）
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
                    val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / (size / 2f)
                    
                    val r = ((Math.sin(angle + dist * 10) * 0.5 + 0.5) * 255).toInt()
                    val g = ((Math.cos(angle + dist * 8) * 0.5 + 0.5) * 255).toInt()
                    val b = 128
                    colors[y * size + x] = 0xFF000000.toInt() or (r shl 16) or (g shl 8) or b
                }
            }
            
            return Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成砖墙纹理（用于演示纹理重复）
         */
        private fun createBrickTexture(): Bitmap {
            val width = 64
            val height = 64
            val colors = IntArray(width * height)
            
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val brickH = 16
                    val brickW = 32
                    val row = y / brickH
                    val offset = if (row % 2 == 0) 0 else brickW / 2
                    val bx = (x + offset) % brickW
                    val by = y % brickH
                    
                    val isMortar = bx < 2 || by < 2
                    colors[y * width + x] = if (isMortar) {
                        0xFF888888.toInt()  // 灰色砂浆
                    } else {
                        0xFFAA4422.toInt()  // 红砖
                    }
                }
            }
            
            return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成渐变纹理（用于演示纹理缩放）
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
    
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var textureCoordHandle: Int = 0
    private var matrixHandle: Int = 0
    private var textureMatrixHandle: Int = 0
    private var textureHandle: Int = 0
    
    private var vertexBuffer: FloatBuffer? = null
    private var repeatVertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val resultMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    
    private val textureMatrix = FloatArray(16)
    
    private var textureId1: Int = 0  // 条纹纹理
    private var textureId2: Int = 0  // 漩涡纹理
    private var textureId3: Int = 0  // 砖墙纹理
    private var textureId4: Int = 0  // 渐变纹理
    
    private var startTime: Long = 0
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        
        startTime = System.currentTimeMillis()
        
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, 0f, 5f,
            0f, 0f, 0f,
            0f, 1f, 0f
        )
        
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        textureCoordHandle = GLES20.glGetAttribLocation(program, "a_TextureCoord")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        textureMatrixHandle = GLES20.glGetUniformLocation(program, "u_TextureMatrix")
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")
        
        textureId1 = createTexture(createStripeTexture(), false)
        textureId2 = createTexture(createSwirlTexture(), false)
        textureId3 = createTexture(createBrickTexture(), true)
        textureId4 = createTexture(createGradientTexture(), false)
        
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_UVS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(RECT_COORDS_AND_UVS)
        vertexBuffer?.position(0)
        
        val rvb = ByteBuffer.allocateDirect(REPEAT_UVS.size * FLOAT_SIZE)
        rvb.order(ByteOrder.nativeOrder())
        repeatVertexBuffer = rvb.asFloatBuffer()
        repeatVertexBuffer?.put(REPEAT_UVS)
        repeatVertexBuffer?.position(0)
        
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    private fun createTexture(bitmap: Bitmap, repeat: Boolean): Int {
        val textureId = IntArray(1)
        
        GLES20.glGenTextures(1, textureId, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
        
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        
        if (repeat) {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        } else {
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        }
        
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        bitmap.recycle()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        
        return textureId[0]
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val aspectRatio = width.toFloat() / height.toFloat()
        
        if (aspectRatio > 1f) {
            Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio * 150f, aspectRatio * 150f,
                -150f, 150f,
                1f, 10f  // near/far 必须是正值，包含相机距离 5
            )
        } else {
            Matrix.orthoM(
                projectionMatrix, 0,
                -150f, 150f,
                -150f / aspectRatio, 150f / aspectRatio,
                1f, 10f  // near/far 必须是正值，包含相机距离 5
            )
        }
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
        
        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(textureHandle, 0)
        
        drawQuad(
            textureId1,
            -80f, 80f,
            textureScrollAnimation(elapsedSeconds)
        )
        
        drawQuad(
            textureId2,
            80f, 80f,
            textureRotateAnimation(elapsedSeconds)
        )
        
        drawQuad(
            textureId3,
            -80f, -80f,
            textureRepeatMatrix(),
            true
        )
        
        drawQuad(
            textureId4,
            80f, -80f,
            textureScaleAnimation(elapsedSeconds)
        )
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
    
    private fun textureScrollAnimation(time: Float): FloatArray {
        Matrix.setIdentityM(textureMatrix, 0)
        val tv = (time * 0.5f) % 1.0f
        Matrix.translateM(textureMatrix, 0, 0f, tv, 0f)
        return textureMatrix.clone()
    }
    
    private fun textureRotateAnimation(time: Float): FloatArray {
        Matrix.setIdentityM(textureMatrix, 0)
        Matrix.translateM(textureMatrix, 0, 0.5f, 0.5f, 0f)
        Matrix.rotateM(textureMatrix, 0, time * 90f, 0f, 0f, 1f)
        Matrix.translateM(textureMatrix, 0, -0.5f, -0.5f, 0f)
        return textureMatrix.clone()
    }
    
    private fun textureRepeatMatrix(): FloatArray {
        Matrix.setIdentityM(textureMatrix, 0)
        return textureMatrix.clone()
    }
    
    private fun textureScaleAnimation(time: Float): FloatArray {
        Matrix.setIdentityM(textureMatrix, 0)
        val scale = 1.0f + Math.sin(time * 2.0).toFloat() * 0.5f
        Matrix.translateM(textureMatrix, 0, 0.5f, 0.5f, 0f)
        Matrix.scaleM(textureMatrix, 0, scale, scale, 1f)
        Matrix.translateM(textureMatrix, 0, -0.5f, -0.5f, 0f)
        return textureMatrix.clone()
    }
    
    private fun drawQuad(textureId: Int, tx: Float, ty: Float, texMatrix: FloatArray, useRepeat: Boolean = false) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
        
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, texMatrix, 0)
        
        val buffer = if (useRepeat) repeatVertexBuffer else vertexBuffer
        drawRectangle(buffer)
    }
    
    private fun drawRectangle(buffer: FloatBuffer?) {
        val stride = (COORDS_PER_VERTEX + UV_PER_VERTEX) * FLOAT_SIZE
        
        buffer?.position(0)
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            stride,
            buffer
        )
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        buffer?.position(COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(
            textureCoordHandle,
            UV_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            stride,
            buffer
        )
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        
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