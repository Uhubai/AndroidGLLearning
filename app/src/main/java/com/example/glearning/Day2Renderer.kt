package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 2 渲染器：绘制渐变色三角形
 * 
 * 学习要点：
 * - 顶点着色器：处理顶点位置和属性
 * - 片段着色器：计算每个像素的颜色
 * - 归一化设备坐标 (NDC)：x, y 范围 [-1, 1]
 * - varying 变量：顶点到片段的插值
 */
class Day2Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day2Renderer"
        
        private const val COORDS_PER_VERTEX = 2
        private const val COLORS_PER_VERTEX = 3
        private const val FLOAT_SIZE = 4
        
        private const val VERTEX_SHADER_CODE = """
            attribute vec4 a_Position;
            attribute vec4 a_Color;
            varying vec4 v_Color;
            
            void main() {
                gl_Position = a_Position;
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
        
        private val TRIANGLE_COORDS_AND_COLORS = floatArrayOf(
            0.0f,  0.5f,   1.0f, 0.0f, 0.0f,
           -0.5f, -0.5f,   0.0f, 1.0f, 0.0f,
            0.5f, -0.5f,   0.0f, 0.0f, 1.0f
        )
    }
    
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var colorHandle: Int = 0
    private var vertexBuffer: FloatBuffer? = null
    
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
        
        val bb = ByteBuffer.allocateDirect(TRIANGLE_COORDS_AND_COLORS.size * FLOAT_SIZE)
        bb.order(ByteOrder.nativeOrder())
        vertexBuffer = bb.asFloatBuffer()
        vertexBuffer?.put(TRIANGLE_COORDS_AND_COLORS)
        vertexBuffer?.position(0)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        GLES20.glUseProgram(program)
        
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
        
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        
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