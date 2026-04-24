/**
 * Day 12: 纹理混合 - 多纹理叠加效果
 *
 * 本渲染器演示：
 * 1. 多纹理混合：同时使用两个纹理
 * 2. 混合模式：线性混合、正片叠底、滤色、叠加
 * 3. 动态混合系数：通过动画控制混合程度
 * 4. 纹理单元管理：GL_TEXTURE0 和 GL_TEXTURE1
 *
 * 绘制内容：
 * - 左上：线性混合（混合系数动画）
 * - 右上：正片叠底（变暗效果）
 * - 左下：滤色（变亮效果）
 * - 右下：叠加（对比增强）
 *
 * 混合模式原理：
 * - 线性：mix(color1, color2, factor)
 * - 正片叠底：color1 * color2
 * - 滤色：1 - (1-color1) * (1-color2)
 * - 叠加：根据底色选择混合方式
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

class Day12Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day12Renderer"
        
        private const val COORDS_PER_VERTEX = 2
        private const val UV_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4
        private const val SHORT_SIZE = 2
        
        /**
         * 顶点着色器代码
         *
         * 与 Day 11 相同，只传递 UV 坐标
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
         * 实现四种混合模式
         *
         * 混合模式说明：
         * - 0: 线性混合 (Linear) - 平滑过渡
         * - 1: 正片叠底 (Multiply) - 相乘变暗
         * - 2: 滤色 (Screen) - 反相相乘变亮
         * - 3: 叠加 (Overlay) - 对比增强
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 v_TextureCoord;
            uniform sampler2D u_Texture1;
            uniform sampler2D u_Texture2;
            uniform float u_MixFactor;
            uniform int u_BlendMode;
            
            void main() {
                vec4 color1 = texture2D(u_Texture1, v_TextureCoord);
                vec4 color2 = texture2D(u_Texture2, v_TextureCoord);
                
                vec4 result;
                
                if (u_BlendMode == 0) {
                    // 线性混合：平滑过渡
                    result = mix(color1, color2, u_MixFactor);
                } else if (u_BlendMode == 1) {
                    // 正片叠底：颜色相乘，结果变暗
                    // 公式：result = color1 * color2
                    result = color1 * color2;
                } else if (u_BlendMode == 2) {
                    // 滤色：反相后相乘再反相，结果变亮
                    // 公式：result = 1 - (1-color1) * (1-color2)
                    result = 1.0 - (1.0 - color1) * (1.0 - color2);
                } else {
                    // 叠加：根据底色决定混合方式
                    // 底色 < 0.5：正片叠底
                    // 底色 >= 0.5：滤色
                    result = mix(color1, color2, u_MixFactor);
                }
                
                gl_FragColor = result;
            }
        """
        
        private val RECT_COORDS_AND_UVS = floatArrayOf(
            -50f,  50f,  0.0f, 1.0f,
            -50f, -50f,  0.0f, 0.0f,
             50f,  50f,  1.0f, 1.0f,
             50f, -50f,  1.0f, 0.0f
        )
        
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
        
        /**
         * 生成砖墙纹理（底色）
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
                        0xFF888888.toInt()
                    } else {
                        0xFFAA4422.toInt()
                    }
                }
            }
            
            return Bitmap.createBitmap(colors, width, height, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成污渍纹理（叠加层）
         */
        private fun createStainTexture(): Bitmap {
            val size = 64
            val colors = IntArray(size * size)
            val centerX = size / 2f
            val centerY = size / 2f
            
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val dist = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat() / (size / 2f)
                    
                    val intensity = (1.0f - dist).coerceAtLeast(0.0f)
                    val alpha = (intensity * 200).toInt()
                    
                    colors[y * size + x] = (alpha shl 24) or 0x00333333.toInt()
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
                    val b = 128.toByte()
                    colors[y * size + x] = 0xFF000000.toInt() or
                        ((r.toInt() and 0xFF) shl 16) or
                        ((g.toInt() and 0xFF) shl 8) or
                        (b.toInt() and 0xFF)
                }
            }
            
            return Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        }
        
        /**
         * 生成棋盘格纹理
         */
        private fun createCheckerboardTexture(): Bitmap {
            val size = 8
            val colors = IntArray(size * size)
            
            for (y in 0 until size) {
                for (x in 0 until size) {
                    val isWhite = (x + y) % 2 == 0
                    colors[y * size + x] = if (isWhite) {
                        0xFFFFFFFF.toInt()
                    } else {
                        0xFF000000.toInt()
                    }
                }
            }
            
            return Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        }
    }
    
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var textureCoordHandle: Int = 0
    private var matrixHandle: Int = 0
    private var texture1Handle: Int = 0
    private var texture2Handle: Int = 0
    private var mixFactorHandle: Int = 0
    private var blendModeHandle: Int = 0
    
    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    
    private val modelMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val resultMatrix = FloatArray(16)
    private val tempMatrix = FloatArray(16)
    
    private var textureId1: Int = 0
    private var textureId2: Int = 0
    
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
        texture1Handle = GLES20.glGetUniformLocation(program, "u_Texture1")
        texture2Handle = GLES20.glGetUniformLocation(program, "u_Texture2")
        mixFactorHandle = GLES20.glGetUniformLocation(program, "u_MixFactor")
        blendModeHandle = GLES20.glGetUniformLocation(program, "u_BlendMode")
        
        textureId1 = createTexture(createBrickTexture())
        textureId2 = createTexture(createStainTexture())
        
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
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)
        
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
                -1f, 1f
            )
        } else {
            Matrix.orthoM(
                projectionMatrix, 0,
                -150f, 150f,
                -150f / aspectRatio, 150f / aspectRatio,
                -1f, 1f
            )
        }
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f
        
        GLES20.glUseProgram(program)
        
        // 激活两个纹理单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glUniform1i(texture1Handle, 0)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glUniform1i(texture2Handle, 1)
        
        // 左上：线性混合（混合系数动画）
        drawQuad(
            -80f, 80f,
            0,  // 线性混合
            (Math.sin(elapsedSeconds) * 0.5 + 0.5).toFloat()
        )
        
        // 右上：正片叠底（变暗）
        drawQuad(
            80f, 80f,
            1,  // 正片叠底
            1.0f
        )
        
        // 左下：滤色（变亮）
        drawQuad(
            -80f, -80f,
            2,  // 滤色
            1.0f
        )
        
        // 右下：叠加（对比增强）
        drawQuad(
            80f, -80f,
            3,  // 叠加
            (Math.sin(elapsedSeconds * 0.5) * 0.5 + 0.5).toFloat()
        )
        
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
    
    private fun drawQuad(tx: Float, ty: Float, blendMode: Int, mixFactor: Float) {
        // 绑定纹理到不同单元
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId1)
        
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId2)
        
        // 计算 MVP 矩阵
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
        
        Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
        Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
        
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        GLES20.glUniform1f(mixFactorHandle, mixFactor)
        GLES20.glUniform1i(blendModeHandle, blendMode)
        
        // 绘制
        val stride = (COORDS_PER_VERTEX + UV_PER_VERTEX) * FLOAT_SIZE
        
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