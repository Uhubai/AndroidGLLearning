/**
 * Day 13-14: 复习 - 纹理 + 变换综合动画
 *
 * 本渲染器演示前 12 天知识的综合运用：
 * 1. MVP 矩阵组合（投影、视图、模型）
 * 2. 纹理变换（滚动、旋转、缩放）
 * 3. 多纹理混合（线性、正片叠底、滤色）
 * 4. 多种动画效果叠加
 *
 * 绘制内容：2×2 网格展示四种综合效果
 * - 左上：平移 + 纹理滚动（水流效果）
 * - 右上：旋转 + 纹理旋转（漩涡效果）
 * - 左下：缩放 + 纹理重复（呼吸砖墙）
 * - 右下：混合模式动画（动态叠加）
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

class Day14Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day14Renderer"
        
        private const val COORDS_PER_VERTEX = 2
        private const val UV_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4
        private const val SHORT_SIZE = 2
        
        /**
         * 顶点着色器：支持 MVP 矩阵和纹理变换矩阵
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
         * 片段着色器：支持双纹理混合
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 v_TextureCoord;
            uniform sampler2D u_Texture1;
            uniform sampler2D u_Texture2;
            uniform float u_MixFactor;
            uniform int u_UseSecondTexture;
            
            void main() {
                vec4 color1 = texture2D(u_Texture1, v_TextureCoord);
                
                if (u_UseSecondTexture == 1) {
                    vec4 color2 = texture2D(u_Texture2, v_TextureCoord);
                    gl_FragColor = mix(color1, color2, u_MixFactor);
                } else {
                    gl_FragColor = color1;
                }
            }
        """
        
        private val RECT_COORDS_AND_UVS = floatArrayOf(
            -50f,  50f,  0.0f, 1.0f,
            -50f, -50f,  0.0f, 0.0f,
             50f,  50f,  1.0f, 1.0f,
             50f, -50f,  1.0f, 0.0f
        )
        
        private val REPEAT_UVS = floatArrayOf(
            -50f,  50f,  0.0f, 2.0f,
            -50f, -50f,  0.0f, 0.0f,
             50f,  50f,  2.0f, 2.0f,
             50f, -50f,  2.0f, 0.0f
        )
        
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
        
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
                    colors[y * width + x] = if (isMortar) 0xFF888888.toInt() else 0xFFAA4422.toInt()
                }
            }
            return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
        }
        
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
        
        private fun createStainTexture(): Bitmap {
            val size = 64
            val colors = IntArray(size * size)
            val centerX = size / 2f
            val centerY = size / 2f
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val dist = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / (size / 2f)
                    val intensity = (1.0f - dist).coerceAtLeast(0.0f)
                    val alpha = (intensity * 200).toInt()
                    colors[y * size + x] = (alpha shl 24) or 0x00333333.toInt()
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
    private var texture1Handle: Int = 0
    private var texture2Handle: Int = 0
    private var mixFactorHandle: Int = 0
    private var useSecondTextureHandle: Int = 0
    
    private var vertexBuffer: FloatBuffer? = null
    private var repeatVertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val resultMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    private val textureMatrix = FloatArray(16)
    
    private var textureId1: Int = 0
    private var textureId2: Int = 0
    private var textureId3: Int = 0
    private var textureId4: Int = 0
    private var textureId5: Int = 0
    
    private var startTime: Long = 0
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        
        startTime = System.currentTimeMillis()
        
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        
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
        texture1Handle = GLES20.glGetUniformLocation(program, "u_Texture1")
        texture2Handle = GLES20.glGetUniformLocation(program, "u_Texture2")
        mixFactorHandle = GLES20.glGetUniformLocation(program, "u_MixFactor")
        useSecondTextureHandle = GLES20.glGetUniformLocation(program, "u_UseSecondTexture")
        
        textureId1 = createTexture(createStripeTexture(), false)
        textureId2 = createTexture(createSwirlTexture(), false)
        textureId3 = createTexture(createBrickTexture(), true)
        textureId4 = createTexture(createGradientTexture(), false)
        textureId5 = createTexture(createStainTexture(), false)
        
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
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio * 150f, aspectRatio * 150f, -150f, 150f, -1f, 1f)
        } else {
            Matrix.orthoM(projectionMatrix, 0, -150f, 150f, -150f / aspectRatio, 150f / aspectRatio, -1f, 1f)
        }
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
        
        GLES20.glUseProgram(program)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(texture1Handle, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glUniform1i(texture2Handle, 1)
        
        // 左上：平移 + 纹理滚动
        drawQuad(
            -80f, 80f,
            textureId1, 0,
            textureScrollMatrix(elapsedSeconds),
            0f, false
        )
        
        // 右上：旋转 + 纹理旋转
        drawQuad(
            80f, 80f,
            textureId2, 0,
            textureRotateMatrix(elapsedSeconds),
            0f, false
        )
        
        // 左下：缩放 + 纹理重复
        drawQuad(
            -80f, -80f,
            textureId3, 0,
            textureIdentityMatrix(),
            0f, true
        )
        
        // 右下：混合模式动画
        drawQuad(
            80f, -80f,
            textureId4, textureId5,
            textureIdentityMatrix(),
            (sin(elapsedSeconds) * 0.5f + 0.5f),
            false
        )
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
    
    private fun textureScrollMatrix(time: Float): FloatArray {
        Matrix.setIdentityM(textureMatrix, 0)
        Matrix.translateM(textureMatrix, 0, 0f, (time * 0.5f) % 1.0f, 0f)
        return textureMatrix.clone()
    }
    
    private fun textureRotateMatrix(time: Float): FloatArray {
        Matrix.setIdentityM(textureMatrix, 0)
        Matrix.translateM(textureMatrix, 0, 0.5f, 0.5f, 0f)
        Matrix.rotateM(textureMatrix, 0, time * 90f, 0f, 0f, 1f)
        Matrix.translateM(textureMatrix, 0, -0.5f, -0.5f, 0f)
        return textureMatrix.clone()
    }
    
    private fun textureIdentityMatrix(): FloatArray {
        Matrix.setIdentityM(textureMatrix, 0)
        return textureMatrix.clone()
    }
    
    private fun drawQuad(tx: Float, ty: Float, tex1: Int, tex2: Int, texMatrix: FloatArray, mixFactor: Float, useRepeat: Boolean) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex1)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex2)
        
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
        
        val scale = if (useRepeat) 1.0f + sin(System.currentTimeMillis() / 1000f * 2.0f) * 0.2f else 1.0f
        Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)
        
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, texMatrix, 0)
        GLES20.glUniform1f(mixFactorHandle, mixFactor)
        GLES20.glUniform1i(useSecondTextureHandle, if (mixFactor > 0f) 1 else 0)
        
        val buffer = if (useRepeat) repeatVertexBuffer else vertexBuffer
        drawRectangle(buffer)
    }
    
    private fun drawRectangle(buffer: FloatBuffer?) {
        val stride = (COORDS_PER_VERTEX + UV_PER_VERTEX) * FLOAT_SIZE
        
        buffer?.position(0)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, stride, buffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        buffer?.position(COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(textureCoordHandle, UV_PER_VERTEX, GLES20.GL_FLOAT, false, stride, buffer)
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