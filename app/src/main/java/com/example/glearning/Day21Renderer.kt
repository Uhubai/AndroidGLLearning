/**
 * Day 21: 扩展 UI 控件 - 复选框、单选框、下拉菜单
 *
 * 本渲染器演示：
 * 1. OpenGLCheckBox 复选框控件
 * 2. OpenGLRadioButton 单选框控件
 * 3. OpenGLDropdown 下拉菜单控件
 * 4. 完整控件库的综合应用
 *
 * 绘制内容：全屏滤镜效果 + 底部完整控制面板
 *
 * 关键概念：
 * - 复选框：布尔值开关，勾选/取消勾选
 * - 单选框：互斥选择，一组中只能选一个
 * - 下拉菜单：展开/收起，选项列表
 */
package com.example.glearning

import android.opengl.GLES20
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL 复选框
 */
class OpenGLCheckBox(
    val x: Float, val y: Float,
    val label: String,
    var isChecked: Boolean = false
) {
    fun containsPoint(px: Float, py: Float): Boolean {
        return px >= x && px <= x + 30f && py >= y && py <= y + 30f
    }
    
    fun toggle() {
        isChecked = !isChecked
    }
}

/**
 * OpenGL 单选框
 */
class OpenGLRadioButton(
    val x: Float, val y: Float,
    val label: String,
    val groupName: String,
    var isSelected: Boolean = false
) {
    fun containsPoint(px: Float, py: Float): Boolean {
        return px >= x && px <= x + 30f && py >= y && py <= y + 30f
    }
    
    fun select() {
        isSelected = true
    }
    
    fun deselect() {
        isSelected = false
    }
}

/**
 * OpenGL 下拉菜单
 */
class OpenGLDropdown(
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val options: List<String>,
    var selectedIndex: Int = 0
) {
    var isExpanded = false
    
    fun containsPoint(px: Float, py: Float): Boolean {
        if (isExpanded) {
            return px >= x && px <= x + width && py >= y - options.size * height && py <= y + height
        }
        return px >= x && px <= x + width && py >= y && py <= y + height
    }
    
    fun toggle() {
        isExpanded = !isExpanded
    }
    
    fun selectOption(index: Int) {
        if (index >= 0 && index < options.size) {
            selectedIndex = index
            isExpanded = false
        }
    }
}

/**
 * Day21Renderer: 扩展 UI 控件渲染器
 */
class Day21Renderer : BaseFilterRenderer() {
    
    companion object {
        private const val TAG = "Day21Renderer"
    }
    
    // UI 控件
    private val checkBoxes = mutableListOf<OpenGLCheckBox>()
    private val radioButtons = mutableListOf<OpenGLRadioButton>()
    private val dropdowns = mutableListOf<OpenGLDropdown>()
    
    // 滤镜参数
    private var brightness = 0.2f
    private var lastBrightness = Float.MAX_VALUE
    private var uBrightness = 0
    
    init {
        // 复选框
        checkBoxes.add(OpenGLCheckBox(20f, 80f, "启用亮度"))
        checkBoxes.add(OpenGLCheckBox(20f, 50f, "启用对比度"))
        
        // 单选框（滤镜类型组）
        radioButtons.add(OpenGLRadioButton(150f, 80f, "原图", "filter", true))
        radioButtons.add(OpenGLRadioButton(150f, 50f, "灰度", "filter", false))
        
        // 下拉菜单（预设参数）
        dropdowns.add(OpenGLDropdown(280f, 65f, 150f, 30f, listOf("默认", "明亮", "暗淡", "自定义")))
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
        
        // 调用基类绘制
        super.onDrawFrame(gl)
    }
    
    /**
     * 处理触摸事件
     */
    fun handleTouch(px: Float, py: Float): Boolean {
        // 检查下拉菜单
        for (dropdown in dropdowns) {
            if (dropdown.containsPoint(px, py)) {
                if (dropdown.isExpanded) {
                    // 选择选项
                    val index = ((dropdown.y - py) / dropdown.height).toInt()
                    dropdown.selectOption(index)
                } else {
                    dropdown.toggle()
                }
                return true
            }
        }
        
        // 检查复选框
        for (checkBox in checkBoxes) {
            if (checkBox.containsPoint(px, py)) {
                checkBox.toggle()
                return true
            }
        }
        
        // 检查单选框
        for (radioButton in radioButtons) {
            if (radioButton.containsPoint(px, py)) {
                // 取消同组其他单选框
                for (rb in radioButtons) {
                    if (rb.groupName == radioButton.groupName) {
                        rb.deselect()
                    }
                }
                radioButton.select()
                return true
            }
        }
        
        return false
    }
}
