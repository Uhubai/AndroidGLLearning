/**
 * Day 16: 基础滤镜 - 亮度/对比度/饱和度可调
 *
 * 本渲染器演示：
 * 1. 亮度调节：线性增加/减少 RGB 值
 * 2. 对比度调节：围绕中点 (0.5) 缩放 RGB 值
 * 3. 饱和度调节：灰度值与原始颜色的线性混合
 * 4. BaseFilterRenderer 基类的使用
 * 5. 参数缓存和更新机制
 *
 * 绘制内容：全屏应用亮度/对比度/饱和度滤镜
 *
 * 关键概念：
 * - 亮度：color.rgb += brightness（范围 -1.0 ~ 1.0）
 * - 对比度：color.rgb = (color.rgb - 0.5) * contrast + 0.5（范围 0.0 ~ 2.0）
 * - 饱和度：mix(gray, color.rgb, saturation)，gray = dot(color.rgb, vec3(0.299, 0.587, 0.114))
 * - 参数缓存：只在值变化时更新 uniform，减少 GL 调用
 */
package com.example.glearning

import android.opengl.GLES20
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day16Renderer : BaseFilterRenderer() {
    
    companion object {
        private const val TAG = "Day16Renderer"
        
        /**
         * 片段着色器：亮度/对比度/饱和度调节
         *
         * 关键变量：
         * - u_Texture: 输入纹理（sampler2D）
         * - u_Brightness: 亮度偏移（uniform, float, 范围 -1.0 ~ 1.0）
         * - u_Contrast: 对比度因子（uniform, float, 范围 0.0 ~ 2.0）
         * - u_Saturation: 饱和度因子（uniform, float, 范围 0.0 ~ 2.0）
         *
         * 工作流程：
         * 1. 从纹理采样获取原始颜色
         * 2. 应用亮度调节：线性偏移
         * 3. 应用对比度调节：围绕中点缩放
         * 4. 应用饱和度调节：灰度混合
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 v_TextureCoord;
            uniform sampler2D u_Texture;
            uniform float u_Brightness;
            uniform float u_Contrast;
            uniform float u_Saturation;
            
            void main() {
                // 步骤 1：从纹理采样
                vec4 color = texture2D(u_Texture, v_TextureCoord);
                
                // 步骤 2：应用亮度调节
                // 线性偏移：正值增亮，负值减暗
                color.rgb += u_Brightness;
                
                // 步骤 3：应用对比度调节
                // 围绕中点 (0.5) 缩放：>1 增强对比，<1 降低对比
                color.rgb = (color.rgb - 0.5) * u_Contrast + 0.5;
                
                // 步骤 4：应用饱和度调节
                // 计算灰度值（人眼感知的亮度）
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                // 混合灰度和原始颜色：0 = 完全灰度，1 = 原始颜色，>1 = 增强饱和
                color.rgb = mix(vec3(gray), color.rgb, u_Saturation);
                
                // 确保颜色值在有效范围 [0, 1]
                gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
            }
        """
    }
    
    // 滤镜参数
    private var brightness = 0f      // 范围 -1.0 ~ 1.0，0 表示无变化
    private var contrast = 1.0f      // 范围 0.0 ~ 2.0，1 表示无变化
    private var saturation = 1.0f    // 范围 0.0 ~ 2.0，1 表示无变化
    
    // 缓存上次值（用于参数变化检测）
    private var lastBrightness = Float.MAX_VALUE
    private var lastContrast = Float.MAX_VALUE
    private var lastSaturation = Float.MAX_VALUE
    
    // uniform 句柄
    private var uBrightness = 0
    private var uContrast = 0
    private var uSaturation = 0
    
    /**
     * 返回片段着色器代码
     *
     * @return 亮度/对比度/饱和度片段着色器
     */
    override fun getFragmentShader(): String = FRAGMENT_SHADER_CODE
    
    /**
     * 设置滤镜特定的 uniform 句柄
     *
     * 获取亮度、对比度、饱和度 uniform 的位置。
     *
     * @param program 着色器程序 ID
     */
    override fun setupUniforms(program: Int) {
        uBrightness = GLES20.glGetUniformLocation(program, "u_Brightness")
        uContrast = GLES20.glGetUniformLocation(program, "u_Contrast")
        uSaturation = GLES20.glGetUniformLocation(program, "u_Saturation")
    }
    
    /**
     * 设置亮度值
     *
     * @param value 亮度值，范围 -1.0 ~ 1.0
     */
    fun setBrightness(value: Float) {
        brightness = value.coerceIn(-1.0f, 1.0f)
    }
    
    /**
     * 设置对比度值
     *
     * @param value 对比度值，范围 0.0 ~ 2.0
     */
    fun setContrast(value: Float) {
        contrast = value.coerceIn(0.0f, 2.0f)
    }
    
    /**
     * 设置饱和度值
     *
     * @param value 饱和度值，范围 0.0 ~ 2.0
     */
    fun setSaturation(value: Float) {
        saturation = value.coerceIn(0.0f, 2.0f)
    }
    
    /**
     * 每帧绘制时调用，更新滤镜参数
     *
     * 使用 updateUniformIfNeeded() 只在参数变化时更新 uniform，
     * 减少不必要的 GL 调用开销。
     */
    override fun onDrawFrame(gl: GL10?) {
        // 更新 uniform 参数（只在值变化时）
        lastBrightness = updateUniformIfNeeded(uBrightness, brightness, lastBrightness)
        lastContrast = updateUniformIfNeeded(uContrast, contrast, lastContrast)
        lastSaturation = updateUniformIfNeeded(uSaturation, saturation, lastSaturation)
        
        // 调用基类绘制
        super.onDrawFrame(gl)
    }
}
