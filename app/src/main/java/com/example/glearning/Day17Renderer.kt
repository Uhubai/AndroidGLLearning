/**
 * Day 17: 基础滤镜 - 灰度/反色/Sepia 经典滤镜
 *
 * 本渲染器演示：
 * 1. 灰度滤镜：使用亮度加权平均 (0.299, 0.587, 0.114)
 * 2. 反色滤镜：RGB 值取反 (1.0 - color)
 * 3. Sepia 滤镜：棕褐色调，模拟老照片效果
 * 4. 滤镜类型切换（uniform int 选择器）
 *
 * 绘制内容：全屏应用选中的经典滤镜
 *
 * 关键概念：
 * - 灰度：gray = dot(color.rgb, vec3(0.299, 0.587, 0.114))
 * - 反色：color.rgb = 1.0 - color.rgb
 * - Sepia：使用固定矩阵变换 RGB 值
 * - 滤镜切换：使用 uniform int 在片段着色器中分支
 */
package com.example.glearning

import android.opengl.GLES20
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day17Renderer : BaseFilterRenderer() {
    
    companion object {
        private const val TAG = "Day17Renderer"
        
        /**
         * 片段着色器：灰度/反色/Sepia 经典滤镜
         *
         * 关键变量：
         * - u_FilterType: 滤镜类型选择器（uniform int）
         *   0 = 原图，1 = 灰度，2 = 反色，3 = Sepia
         *
         * Sepia 矩阵：
         * - R = 0.393*r + 0.769*g + 0.189*b
         * - G = 0.349*r + 0.686*g + 0.168*b
         * - B = 0.272*r + 0.534*g + 0.131*b
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 v_TextureCoord;
            uniform sampler2D u_Texture;
            uniform int u_FilterType;
            
            void main() {
                vec4 color = texture2D(u_Texture, v_TextureCoord);
                
                if (u_FilterType == 1) {
                    // 灰度滤镜
                    float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                    color.rgb = vec3(gray);
                } else if (u_FilterType == 2) {
                    // 反色滤镜
                    color.rgb = 1.0 - color.rgb;
                } else if (u_FilterType == 3) {
                    // Sepia 棕褐色滤镜
                    float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
                    float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
                    float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;
                    color.rgb = vec3(r, g, b);
                }
                
                gl_FragColor = vec4(clamp(color.rgb, 0.0, 1.0), color.a);
            }
        """
    }
    
    // 滤镜类型：0=原图，1=灰度，2=反色，3=Sepia
    private var filterType = 0
    private var lastFilterType = -1
    
    // uniform 句柄
    private var uFilterType = 0
    
    /**
     * 返回片段着色器代码
     */
    override fun getFragmentShader(): String = FRAGMENT_SHADER_CODE
    
    /**
     * 设置滤镜特定的 uniform 句柄
     */
    override fun setupUniforms(program: Int) {
        uFilterType = GLES20.glGetUniformLocation(program, "u_FilterType")
    }
    
    /**
     * 设置滤镜类型
     *
     * @param type 滤镜类型（0=原图，1=灰度，2=反色，3=Sepia）
     */
    fun setFilterType(type: Int) {
        filterType = type.coerceIn(0, 3)
    }
    
    /**
     * 每帧绘制时调用，更新滤镜参数
     */
    override fun onDrawFrame(gl: GL10?) {
        // 更新滤镜类型（只在值变化时）
        if (filterType != lastFilterType) {
            GLES20.glUniform1i(uFilterType, filterType)
            lastFilterType = filterType
        }
        
        // 调用基类绘制
        super.onDrawFrame(gl)
    }
}
