/**
 * Day 3: 索引缓冲区（IBO） - 高效绘制矩形
 *
 * 本渲染器演示：
 * 1. 索引缓冲区（Index Buffer Object, IBO）：存储顶点索引
 * 2. glDrawElements：通过索引绘制，减少顶点数据重复
 * 3. 顶点复用：矩形的 4 个顶点 vs 6 个顶点
 * 4. GL_TRIANGLES 绘制模式：每 3 个索引组成一个三角形
 *
 * 绘制内容：一个带顶点颜色渐变的矩形
 * - 左上红色，左下绿色，右上蓝色，右下黄色
 * - GPU 自动在矩形内部插值颜色
 *
 * 索引缓冲的优势：
 * - 减少数据重复：矩形只需 4 个顶点而非 6 个（2 个三角形）
 * - 提高渲染效率：GPU 缓存顶点数据，减少内存访问
 * - 复杂图形更高效：大型网格可节省大量顶点数据
 */
package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day3Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day3Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val COLORS_PER_VERTEX = 3   // 每个顶点有 3 个颜色分量 (r, g, b)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        private const val SHORT_SIZE = 2          // Short 类型占用 2 字节
        
        /**
         * 顶点着色器代码
         *
         * 与 Day 2 相同的着色器，无矩阵变换
         * 直接使用归一化设备坐标（NDC）
         *
         * 变量说明：
         * - attribute: 顶点属性，每个顶点不同的数据
         * - varying: 传递给片段着色器的插值变量
         */
        private const val VERTEX_SHADER_CODE = """
            attribute vec4 a_Position;
            attribute vec4 a_Color;
            varying vec4 v_Color;
            
            void main() {
                gl_Position = a_Position;
                v_Color = a_Color;
            }
        """
        
        /**
         * 片段着色器代码
         *
         * 接收插值颜色并输出为像素颜色
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec4 v_Color;
            
            void main() {
                gl_FragColor = v_Color;
            }
        """
        
        /**
         * 矩形顶点坐标和颜色数据
         *
         * 数据结构：交错存储
         * [x, y, r, g, b] × 4 个顶点 = 20 个浮点数
         *
         * 顶点布局（NDC）：
         *   顶点0 (-0.5, 0.5) 红色 ──── 顶点2 (0.5, 0.5) 蓝色
         *   │                                    │
         *   │                                    │
         *   顶点1 (-0.5, -0.5) 绿色 ──── 顶点3 (0.5, -0.5) 黄色
         *
         * 注意：只有 4 个顶点，但需要 2 个三角形（6 个顶点索引）
         */
        private val RECT_COORDS_AND_COLORS = floatArrayOf(
            // x,     y,      r,     g,     b
            -0.5f,  0.5f,  1.0f, 0.0f, 0.0f,  // 顶点0: 左上，红色
            -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  // 顶点1: 左下，绿色
             0.5f,  0.5f,  0.0f, 0.0f, 1.0f,  // 顶点2: 右上，蓝色
             0.5f, -0.5f,  1.0f, 1.0f, 0.0f   // 顶点3: 右下，黄色
        )
        
        /**
         * 矩形顶点索引数据
         *
         * 索引的作用：指定使用哪些顶点组成三角形
         *
         * 矩形由 2 个三角形组成：
         * - 三角形1: 顶点 0, 1, 2（左上、左下、右上）
         * - 三角形2: 顶点 1, 3, 2（左下、右下、右上）
         *
         * 顶点复用：
         * - 顶点1 和 顶点2 被两个三角形共享
         * - 使用索引缓冲只需存储 4 个顶点 + 6 个索引
         * - 不用索引则需要存储 6 个重复顶点
         *
         * 索引顺序：遵循逆时针方向（正面）
         * OpenGL 默认：逆时针顶点顺序为正面，顺时针为背面
         */
        private val RECT_INDICES = shortArrayOf(
            0, 1, 2,  // 第一个三角形
            1, 3, 2   // 第二个三角形
        )
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性句柄
    private var colorHandle: Int = 0          // 顶点颜色属性句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和颜色缓冲区
    private var indexBuffer: ShortBuffer? = null    // 顶点索引缓冲区
    
    /**
     * Surface 创建时的初始化回调
     *
     * 主要任务：
     * 1. 编译着色器并创建程序
     * 2. 获取属性句柄
     * 3. 创建顶点缓冲区（VBO）
     * 4. 创建索引缓冲区（IBO）
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        // 创建并链接着色器程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 获取属性句柄
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(program, "a_Color")
        
        // 创建顶点数据缓冲区（存储坐标和颜色）
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_COLORS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(RECT_COORDS_AND_COLORS)
        vertexBuffer?.position(0)
        
        // 创建索引数据缓冲区（存储顶点索引）
        // 索引使用 Short 类型（GL_UNSIGNED_SHORT）
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    /**
     * Surface 尺寸变化时的回调
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }
    
    /**
     * 每帧绘制回调
     *
     * 主要任务：
     * 1. 设置顶点属性指针
     * 2. 使用索引绘制（glDrawElements）
     * 3. 清理资源
     */
    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        GLES20.glUseProgram(program)
        
        // 计算步幅：一个顶点数据的总字节数
        val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE
        
        // 设置顶点位置属性
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
        
        // 设置顶点颜色属性
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
        
        // 使用索引绘制矩形
        // glDrawElements vs glDrawArrays:
        // - glDrawArrays: 直接按顺序使用顶点（Day 2）
        // - glDrawElements: 按索引顺序使用顶点（Day 3）
        //
        // 参数说明：
        // - GL_TRIANGLES: 绘制模式，每 3 个索引组成三角形
        // - RECT_INDICES.size: 索引数量 = 6（2 个三角形）
        // - GL_UNSIGNED_SHORT: 索引数据类型
        // - indexBuffer: 索引数据源
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            RECT_INDICES.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        
        // 清理：禁用顶点属性
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
    
    /**
     * 编译着色器
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}