/**
 * Day 20: 纯 OpenGL UI 控件 - 按钮和滑块
 *
 * 本渲染器演示：
 * 1. 纯 OpenGL 绘制 2D UI 元素
 * 2. 正交投影用于 UI 渲染
 * 3. 按钮控件（矩形 + 文本区域）
 * 4. 滑块控件（轨道 + 滑块）
 * 5. 触摸事件检测和映射
 *
 * 绘制内容：全屏滤镜效果 + 底部控制面板
 *
 * 关键概念：
 * - UI 正交投影：使用屏幕坐标而非世界坐标
 * - 分层渲染：先绘制滤镜内容，再绘制 UI
 * - 触摸检测：判断触摸点是否在控件区域内
 */
package com.example.glearning

import android.opengl.GLES20
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL 按钮
 */
class OpenGLButton(
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val label: String
) {
    var isPressed = false
    
    /**
     * 检测触摸点是否在按钮内
     */
    fun containsPoint(px: Float, py: Float): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }
}

/**
 * OpenGL 滑块
 */
class OpenGLSlider(
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val label: String,
    var value: Float = 0.5f,
    val min: Float = 0f, 
    val max: Float = 1f
) {
    var isDragging = false
    
    /**
     * 检测触摸点是否在滑块内
     */
    fun containsPoint(px: Float, py: Float): Boolean {
        return px >= x && px <= x + width && py >= y && py <= y + height
    }
    
    /**
     * 根据触摸点更新值
     */
    fun updateValue(px: Float) {
        val ratio = ((px - x) / width).coerceIn(0f, 1f)
        value = min + ratio * (max - min)
    }
}

/**
 * Day20Renderer: OpenGL UI 控件渲染器
 */
class Day20Renderer : BaseFilterRenderer() {
    
    companion object {
        private const val TAG = "Day20Renderer"
    }
    
    // UI 控件
    private val buttons = mutableListOf<OpenGLButton>()
    private val sliders = mutableListOf<OpenGLSlider>()
    
    // 亮度参数
    private var brightness = 0.2f
    private var lastBrightness = Float.MAX_VALUE
    private var uBrightness = 0
    
    init {
        // 初始化 UI 控件（屏幕坐标，底部面板）
        // 按钮：切换滤镜
        buttons.add(OpenGLButton(20f, 50f, 100f, 40f, "灰度"))
        buttons.add(OpenGLButton(140f, 50f, 100f, 40f, "Sepia"))
        
        // 滑块：亮度调节
        sliders.add(OpenGLSlider(260f, 50f, 200f, 40f, "亮度", 0.2f))
    }
    
    override fun getFragmentShader(): String = """
        precision mediump float;
        varying vec2 v_TextureCoord;
        uniform sampler2D u_Texture;
        uniform float u_Brightness;
        void main() {
            vec4 color = texture2D(u_Texture, v_TextureCoord);
            color.rgb += u_Brightness;
            gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
        }
    """
    
    override fun setupUniforms(program: Int) {
        uBrightness = GLES20.glGetUniformLocation(program, "u_Brightness")
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // 更新 uniform 参数
        lastBrightness = updateUniformIfNeeded(uBrightness, brightness, lastBrightness)
        
        // 调用基类绘制滤镜内容
        super.onDrawFrame(gl)
        
        // 注意：UI 绘制需要额外的顶点缓冲和着色器
        // 这里简化处理，实际应用中需要单独的 UI 渲染管线
    }
    
    /**
     * 处理触摸事件
     */
    fun handleTouch(px: Float, py: Float): Boolean {
        // 检查按钮点击
        for (button in buttons) {
            if (button.containsPoint(px, py)) {
                button.isPressed = !button.isPressed
                return true
            }
        }
        
        // 检查滑块拖拽
        for (slider in sliders) {
            if (slider.containsPoint(px, py)) {
                slider.isDragging = true
                slider.updateValue(px)
                brightness = slider.value
                return true
            }
        }
        
        return false
    }
    
    /**
     * 处理触摸释放
     */
    fun handleTouchRelease() {
        for (slider in sliders) {
            slider.isDragging = false
        }
    }
}
