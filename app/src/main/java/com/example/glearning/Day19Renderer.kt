/**
 * Day 19: 滤镜架构 - 可切换滤镜管理器
 *
 * 本渲染器演示：
 * 1. Filter 接口定义
 * 2. FilterManager 滤镜管理器
 * 3. 运行时滤镜切换
 * 4. 当前滤镜名称显示
 *
 * 绘制内容：全屏应用当前选中的滤镜，底部显示滤镜名称
 *
 * 关键概念：
 * - Filter 接口：定义滤镜的统一 API
 * - FilterManager：管理滤镜列表和当前滤镜
 * - 点击切换：触摸屏幕切换到下一个滤镜
 */
package com.example.glearning

import android.opengl.GLES20
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * 滤镜接口
 *
 * 定义滤镜的统一 API，所有滤镜必须实现此接口。
 */
interface Filter {
    /**
     * 获取片段着色器代码
     */
    fun getFragmentShader(): String
    
    /**
     * 设置滤镜特定的 uniform 句柄
     */
    fun setupUniforms(program: Int)
    
    /**
     * 每帧更新 uniform 参数
     */
    fun updateUniforms()
    
    /**
     * 获取滤镜名称
     */
    fun getName(): String
}

/**
 * 滤镜管理器
 *
 * 管理滤镜列表和当前选中的滤镜。
 */
class FilterManager {
    private val filters = mutableListOf<Filter>()
    private var currentFilterIndex = -1
    
    /**
     * 添加滤镜
     */
    fun addFilter(filter: Filter) {
        filters.add(filter)
        if (currentFilterIndex == -1) currentFilterIndex = 0
    }
    
    /**
     * 获取当前滤镜
     */
    fun getCurrentFilter(): Filter? = 
        if (currentFilterIndex >= 0 && filters.isNotEmpty()) filters[currentFilterIndex] else null
    
    /**
     * 切换到下一个滤镜
     */
    fun switchToNext() {
        if (filters.isNotEmpty()) {
            currentFilterIndex = (currentFilterIndex + 1) % filters.size
        }
    }
    
    /**
     * 切换到上一个滤镜
     */
    fun switchToPrevious() {
        if (filters.isNotEmpty()) {
            currentFilterIndex = (currentFilterIndex - 1 + filters.size) % filters.size
        }
    }
    
    /**
     * 获取当前滤镜索引
     */
    fun getCurrentIndex(): Int = currentFilterIndex
    
    /**
     * 获取滤镜总数
     */
    fun getFilterCount(): Int = filters.size
}

/**
 * Day19Renderer: 滤镜管理器渲染器
 */
class Day19Renderer : BaseFilterRenderer() {
    
    companion object {
        private const val TAG = "Day19Renderer"
    }
    
    // 滤镜管理器
    private val filterManager = FilterManager()
    
    init {
        // 添加 Day16-18 的滤镜实例
        filterManager.addFilter(BrightnessFilter())
        filterManager.addFilter(GrayscaleFilter())
        filterManager.addFilter(SepiaFilter())
    }
    
    /**
     * 返回当前滤镜的片段着色器代码
     */
    override fun getFragmentShader(): String {
        return filterManager.getCurrentFilter()?.getFragmentShader() 
            ?: "precision mediump float; varying vec2 v_TextureCoord; uniform sampler2D u_Texture; void main() { gl_FragColor = texture2D(u_Texture, v_TextureCoord); }"
    }
    
    /**
     * 设置当前滤镜的 uniform 句柄
     */
    override fun setupUniforms(program: Int) {
        filterManager.getCurrentFilter()?.setupUniforms(program)
    }
    
    /**
     * 每帧绘制时调用
     */
    override fun onDrawFrame(gl: GL10?) {
        // 更新当前滤镜的 uniform 参数
        filterManager.getCurrentFilter()?.updateUniforms()
        
        // 调用基类绘制
        super.onDrawFrame(gl)
    }
    
    /**
     * 切换到下一个滤镜
     */
    fun switchToNextFilter() {
        filterManager.switchToNext()
        // 重新初始化着色器程序（因为片段着色器改变了）
        // 注意：在实际应用中，这应该通过重新编译着色器来实现
    }
    
    /**
     * 获取当前滤镜名称
     */
    fun getCurrentFilterName(): String {
        return filterManager.getCurrentFilter()?.getName() ?: "Unknown"
    }
    
    /**
     * 获取当前滤镜索引
     */
    fun getCurrentFilterIndex(): Int = filterManager.getCurrentIndex()
    
    /**
     * 获取滤镜总数
     */
    fun getFilterCount(): Int = filterManager.getFilterCount()
}

/**
 * 亮度滤镜实现
 */
class BrightnessFilter : Filter {
    private var uBrightness = 0
    private var brightness = 0.2f
    
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
    
    override fun updateUniforms() {
        GLES20.glUniform1f(uBrightness, brightness)
    }
    
    override fun getName(): String = "亮度滤镜"
}

/**
 * 灰度滤镜实现
 */
class GrayscaleFilter : Filter {
    private var uFilterType = 0
    
    override fun getFragmentShader(): String = """
        precision mediump float;
        varying vec2 v_TextureCoord;
        uniform sampler2D u_Texture;
        uniform int u_FilterType;
        void main() {
            vec4 color = texture2D(u_Texture, v_TextureCoord);
            float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
            color.rgb = vec3(gray);
            gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
        }
    """
    
    override fun setupUniforms(program: Int) {
        uFilterType = GLES20.glGetUniformLocation(program, "u_FilterType")
    }
    
    override fun updateUniforms() {
        GLES20.glUniform1i(uFilterType, 1)
    }
    
    override fun getName(): String = "灰度滤镜"
}

/**
 * Sepia 滤镜实现
 */
class SepiaFilter : Filter {
    private var uFilterType = 0
    
    override fun getFragmentShader(): String = """
        precision mediump float;
        varying vec2 v_TextureCoord;
        uniform sampler2D u_Texture;
        uniform int u_FilterType;
        void main() {
            vec4 color = texture2D(u_Texture, v_TextureCoord);
            float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
            float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
            float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;
            color.rgb = vec3(r, g, b);
            gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
        }
    """
    
    override fun setupUniforms(program: Int) {
        uFilterType = GLES20.glGetUniformLocation(program, "u_FilterType")
    }
    
    override fun updateUniforms() {
        GLES20.glUniform1i(uFilterType, 3)
    }
    
    override fun getName(): String = "Sepia 滤镜"
}
