/**
 * Day 18: 基础滤镜 - 色调/色温调节
 *
 * 本渲染器演示：
 * 1. RGB↔HSV 颜色空间转换
 * 2. 色调偏移（Hue Shift）
 * 3. 色温调节（冷暖色调）
 *
 * 绘制内容：全屏应用色调/色温滤镜
 *
 * 关键概念：
 * - HSV：Hue（色调 0-360°），Saturation（饱和度），Value（明度）
 * - 色调偏移：修改 H 分量实现颜色轮转
 * - 色温调节：暖色调增加红色，冷色调增加蓝色
 */
package com.example.glearning

import android.opengl.GLES20
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day18Renderer : BaseFilterRenderer() {
    
    companion object {
        private const val TAG = "Day18Renderer"
        
        /**
         * 片段着色器：色调/色温调节
         *
         * 关键变量：
         * - u_HueShift: 色调偏移（uniform float，范围 -1.0 ~ 1.0，映射到 -180° ~ 180°）
         * - u_Temperature: 色温（uniform float，范围 -1.0 ~ 1.0，冷 ~ 暖）
         *
         * RGB↔HSV 转换：
         * - rgb2hsv: 将 RGB 转换为 HSV 颜色空间
         * - hsv2rgb: 将 HSV 转换回 RGB 颜色空间
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 v_TextureCoord;
            uniform sampler2D u_Texture;
            uniform float u_HueShift;
            uniform float u_Temperature;
            
            // RGB to HSV 转换
            vec3 rgb2hsv(vec3 c) {
                vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
                vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
                vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
                float d = q.x - min(q.w, q.y);
                float e = 1.0e-10;
                return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
            }
            
            // HSV to RGB 转换
            vec3 hsv2rgb(vec3 c) {
                vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
                return c.z * mix(vec3(1.0), rgb, c.y);
            }
            
            void main() {
                vec4 color = texture2D(u_Texture, v_TextureCoord);
                
                // 转换为 HSV
                vec3 hsv = rgb2hsv(color.rgb);
                
                // 应用色调偏移（-180° ~ 180°）
                hsv.x = fract(hsv.x + u_HueShift);
                
                // 应用色温调节
                // 暖色调：增加红色分量
                // 冷色调：增加蓝色分量
                if (u_Temperature > 0.0) {
                    color.rgb = mix(color.rgb, vec3(color.r * 1.2, color.g * 0.9, color.b * 0.8), u_Temperature);
                } else {
                    color.rgb = mix(color.rgb, vec3(color.r * 0.8, color.g * 0.9, color.b * 1.2), -u_Temperature);
                }
                
                // 转换回 RGB
                color.rgb = hsv2rgb(hsv);
                
                gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
            }
        """
    }
    
    // 滤镜参数
    private var hueShift = 0f        // 范围 -1.0 ~ 1.0（映射到 -180° ~ 180°）
    private var temperature = 0f     // 范围 -1.0 ~ 1.0（冷 ~ 暖）
    
    // 缓存上次值
    private var lastHueShift = Float.MAX_VALUE
    private var lastTemperature = Float.MAX_VALUE
    
    // uniform 句柄
    private var uHueShift = 0
    private var uTemperature = 0
    
    /**
     * 返回片段着色器代码
     */
    override fun getFragmentShader(): String = FRAGMENT_SHADER_CODE
    
    /**
     * 设置滤镜特定的 uniform 句柄
     */
    override fun setupUniforms(program: Int) {
        uHueShift = GLES20.glGetUniformLocation(program, "u_HueShift")
        uTemperature = GLES20.glGetUniformLocation(program, "u_Temperature")
    }
    
    /**
     * 设置色调偏移
     *
     * @param value 色调偏移值，范围 -1.0 ~ 1.0
     */
    fun setHueShift(value: Float) {
        hueShift = value.coerceIn(-1.0f, 1.0f)
    }
    
    /**
     * 设置色温
     *
     * @param value 色温值，范围 -1.0 ~ 1.0（负值=冷，正值=暖）
     */
    fun setTemperature(value: Float) {
        temperature = value.coerceIn(-1.0f, 1.0f)
    }
    
    /**
     * 每帧绘制时调用，更新滤镜参数
     */
    override fun onDrawFrame(gl: GL10?) {
        // 更新 uniform 参数（只在值变化时）
        lastHueShift = updateUniformIfNeeded(uHueShift, hueShift, lastHueShift)
        lastTemperature = updateUniformIfNeeded(uTemperature, temperature, lastTemperature)
        
        // 调用基类绘制
        super.onDrawFrame(gl)
    }
}
