package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Day 1 渲染器：绘制纯色背景
 * 
 * 这是 OpenGL ES 最基础的例子。
 * 我们只需要实现 onSurfaceCreated 方法来设置清屏颜色。
 */
class Day1Renderer : GLSurfaceView.Renderer {
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.1f, 0.2f, 0.4f, 1.0f)
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
    
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }
}