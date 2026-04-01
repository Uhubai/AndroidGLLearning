/**
 * Day 1: OpenGL ES 基础 - 清屏操作
 *
 * 本渲染器演示：
 * 1. GLSurfaceView.Renderer 接口的三个回调方法
 * 2. glClearColor - 设置清屏颜色
 * 3. glClear - 执行清屏操作
 * 4. glViewport - 设置视口大小
 *
 * 绘制内容：纯色背景（深蓝色）
 *
 * 这是 OpenGL ES 最基础的示例，不涉及任何图形绘制。
 * 主要目的是理解 OpenGL 渲染流程的基本框架。
 */
package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day1Renderer : GLSurfaceView.Renderer {
    
    /**
     * Surface 创建时的初始化回调
     *
     * 执行时机：GLSurfaceView 创建或重建时调用一次
     * 主要任务：
     * 1. 设置清屏颜色（RGBA 格式）
     * 2. 初始化 OpenGL 资源（本例无其他资源）
     *
     * 注意：此方法只调用一次，适合进行一次性初始化操作
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色为深蓝色
        // 参数：red, green, blue, alpha（范围：0.0f ~ 1.0f）
        // (0.1f, 0.2f, 0.4f, 1.0f) = RGB(26, 51, 102)
        // alpha = 1.0f 表示完全不透明
        GLES20.glClearColor(0.1f, 0.2f, 0.4f, 1.0f)
    }
    
    /**
     * Surface 尺寸变化时的回调
     *
     * 执行时机：Surface 创建后、屏幕旋转、窗口大小改变时调用
     * 主要任务：
     * 1. 设置视口大小（定义渲染区域）
     *
     * 视口（Viewport）：
     * - 定义 OpenGL 渲染结果在屏幕上的显示区域
     * - 参数：x, y, width, height（以像素为单位）
     * - 通常设置为整个 Surface 大小
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口覆盖整个 Surface
        // (0, 0) 表示从左上角开始
        // width, height 表示渲染区域的宽高
        GLES20.glViewport(0, 0, width, height)
    }
    
    /**
     * 每帧绘制回调
     *
     * 执行时机：每秒调用约 60 次（与屏幕刷新率同步）
     * 主要任务：
     * 1. 清除颜色缓冲区
     * 2. 绘制图形内容（本例只清屏）
     *
     * 渲染循环：
     * - GLSurfaceView 自动管理渲染循环
     * - 每帧开始时需要清除上一帧的内容
     * - glClear 用 glClearColor 设置的颜色填充整个屏幕
     */
    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色缓冲区
        // GL_COLOR_BUFFER_BIT：颜色缓冲区的位掩码
        // 清屏后整个屏幕显示 glClearColor 设置的颜色
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    }
}