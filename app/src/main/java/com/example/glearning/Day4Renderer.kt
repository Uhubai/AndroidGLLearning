package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 4 渲染器：正交投影 + 屏幕适配
 * 
 * 学习要点：
 * - 正交投影矩阵：将像素坐标转换为 NDC
 * - 屏幕适配：保持图形比例不变
 * - Matrix 操作：Android 提供的矩阵工具类
 */
class Day4Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day4Renderer"
        
        private const val COORDS_PER_VERTEX = 2
        private const val COLORS_PER_VERTEX = 3
        private const val FLOAT_SIZE = 4
        private const val SHORT_SIZE = 2
        
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
        
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec4 v_Color;
            
            void main() {
                gl_FragColor = v_Color;
            }
        """
        
        private val RECT_COORDS_AND_COLORS = floatArrayOf(
            -100f,  100f,  1.0f, 0.0f, 0.0f,
            -100f, -100f,  0.0f, 1.0f, 0.0f,
             100f,  100f,  0.0f, 0.0f, 1.0f,
             100f, -100f,  1.0f, 1.0f, 0.0f
        )
        
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
    }
    
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var matrixHandle: Int = 0
    private var vertexBuffer: FloatBuffer? = null
    private var indexBuffer: ShortBuffer? = null
    private val projectionMatrix = FloatArray(16)
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(program, "a_Color")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_COLORS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(RECT_COORDS_AND_COLORS)
        vertexBuffer?.position(0)
        
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        
        val aspectRatio = width.toFloat() / height.toFloat()
        
        if (aspectRatio > 1f) {
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio, aspectRatio,
                -1f, 1f,
                -1f, 1f
            )
        } else {
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -1f, 1f,
                -1f / aspectRatio, 1f / aspectRatio,
                -1f, 1f
            )
        }
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        GLES20.glUseProgram(program)
        
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, projectionMatrix, 0)
        
        val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE
        
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
            colorHandle,
            COLORS_PER_VERTEX,
            GLES20.GL_FLOAT,
            false,
            stride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(colorHandle)
        
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            RECT_INDICES.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
    
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}