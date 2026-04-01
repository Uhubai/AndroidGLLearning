/**
 * Day 2: 顶点着色器与片段着色器 - 绘制渐变色三角形
 *
 * 本渲染器演示：
 * 1. 顶点着色器（Vertex Shader）：处理顶点位置和属性
 * 2. 片段着色器（Fragment Shader）：计算每个像素的颜色
 * 3. 归一化设备坐标（NDC）：坐标范围 [-1, 1]
 * 4. varying 变量：顶点到片段的自动插值
 * 5. 顶点缓冲区：存储顶点数据供 GPU 使用
 *
 * 绘制内容：一个带顶点颜色渐变的三角形
 * - 顶部红色，左下绿色，右下蓝色
 * - GPU 自动在三角形内部插值颜色，产生平滑渐变效果
 *
 * OpenGL ES 渲染流程：
 * 顶点数据 → 顶点着色器 → 图元装配 → 光栅化 → 片段着色器 → 像素输出
 */
package com.example.glearning

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day2Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day2Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val COLORS_PER_VERTEX = 3   // 每个顶点有 3 个颜色分量 (r, g, b)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        
        /**
         * 顶点着色器代码
         *
         * 关键概念：
         * - attribute vec4 a_Position: 顶点位置属性，每个顶点不同
         *   vec4 表示 4 个分量 (x, y, z, w)，这里只用 x, y
         * - attribute vec4 a_Color: 顶点颜色属性，每个顶点不同
         * - varying vec4 v_Color: 传递给片段着色器的变量
         *   GPU 会自动在三角形内插值此变量
         *
         * 工作流程：
         * 1. 接收顶点位置 a_Position
         * 2. 直接赋值给 gl_Position（内置输出变量）
         * 3. 将颜色 a_Color 传递给 v_Color
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
         * 关键概念：
         * - precision mediump float: 设置浮点精度为中等
         *   mediump 是性能与质量的平衡选择
         * - varying vec4 v_Color: 从顶点着色器接收的插值颜色
         *   GPU 在光栅化阶段自动计算每个片段的插值值
         *
         * 工作流程：
         * 1. 接收插值后的颜色 v_Color
         * 2. 直接赋值给 gl_FragColor（内置输出变量）
         *
         * 插值原理：
         * - 三角形三个顶点各有不同颜色
         * - GPU 根据片段位置计算加权平均
         * - 离某个顶点越近，该顶点颜色的权重越大
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec4 v_Color;
            
            void main() {
                gl_FragColor = v_Color;
            }
        """
        
        /**
         * 三角形顶点坐标和颜色数据
         *
         * 数据结构：交错存储（坐标+颜色）
         * [x, y, r, g, b] × 3 个顶点 = 15 个浮点数
         *
         * 顶点布局（归一化设备坐标 NDC）：
         *          顶点0 (0.0, 0.5) 红色
         *              │
         *              │
         *              │
         *    顶点1 ────┴──── 顶点2
         *   (-0.5, -0.5)    (0.5, -0.5)
         *      绿色           蓝色
         *
         * NDC 范围：
         * - x: -1.0（左）到 1.0（右）
         * - y: -1.0（下）到 1.0（上）
         * - 超出范围的顶点会被裁剪
         */
        private val TRIANGLE_COORDS_AND_COLORS = floatArrayOf(
            // x,     y,      r,     g,     b
             0.0f,  0.5f,   1.0f, 0.0f, 0.0f,  // 顶点0: 顶部，红色
            -0.5f, -0.5f,   0.0f, 1.0f, 0.0f,  // 顶点1: 左下，绿色
             0.5f, -0.5f,   0.0f, 0.0f, 1.0f   // 顶点2: 右下，蓝色
        )
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性的位置句柄
    private var colorHandle: Int = 0          // 顶点颜色属性的位置句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和颜色数据缓冲区
    
    /**
     * Surface 创建时的初始化回调
     *
     * 执行时机：GLSurfaceView 创建或重建时调用一次
     * 主要任务：
     * 1. 设置清屏颜色
     * 2. 编译顶点和片段着色器
     * 3. 创建并链接着色器程序
     * 4. 获取属性句柄
     * 5. 创建顶点数据缓冲区
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色为黑色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 步骤1：编译着色器
        // GLES20.GL_VERTEX_SHADER: 顶点着色器类型
        // GLES20.GL_FRAGMENT_SHADER: 片段着色器类型
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        // 步骤2：创建着色器程序并链接
        program = GLES20.glCreateProgram()             // 创建空程序对象
        GLES20.glAttachShader(program, vertexShader)   // 附着顶点着色器
        GLES20.glAttachShader(program, fragmentShader) // 附着片段着色器
        GLES20.glLinkProgram(program)                  // 链接程序
        
        // 步骤3：获取属性句柄
        // glGetAttribLocation: 获取 attribute 变量的位置
        // 返回值用于后续设置顶点属性数据
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(program, "a_Color")
        
        // 步骤4：创建顶点数据缓冲区
        // ByteBuffer.allocateDirect: 分配直接内存（Native Memory）
        // 直接内存优势：避免 JVM 堆到 GPU 的数据拷贝，提高性能
        val bb = ByteBuffer.allocateDirect(TRIANGLE_COORDS_AND_COLORS.size * FLOAT_SIZE)
        bb.order(ByteOrder.nativeOrder())         // 设置字节序为本地字节序
        vertexBuffer = bb.asFloatBuffer()         // 转换为 FloatBuffer
        vertexBuffer?.put(TRIANGLE_COORDS_AND_COLORS)  // 写入顶点数据
        vertexBuffer?.position(0)                 // 重置位置指针到开头
    }
    
    /**
     * Surface 尺寸变化时的回调
     *
     * 执行时机：Surface 创建后、屏幕旋转时调用
     * 主要任务：设置视口大小
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口覆盖整个 Surface
        GLES20.glViewport(0, 0, width, height)
    }
    
    /**
     * 每帧绘制回调
     *
     * 执行时机：每秒调用约 60 次
     * 主要任务：
     * 1. 清除屏幕
     * 2. 激活着色器程序
     * 3. 设置顶点属性指针
     * 4. 绘制三角形
     * 5. 清理资源
     */
    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 激活着色器程序
        GLES20.glUseProgram(program)
        
        // 步骤1：设置顶点属性指针
        // stride（步幅）：一个顶点数据占用的总字节数
        // 交错数据格式需要指定步幅让 GPU 跳过其他数据
        val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE
        
        // 设置顶点位置属性
        vertexBuffer?.position(0)  // 定位到位置数据开头
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,     // 每个位置有 2 个分量
            GLES20.GL_FLOAT,       // 数据类型
            false,                 // 不归一化
            stride,                // 步幅
            vertexBuffer           // 数据源
        )
        GLES20.glEnableVertexAttribArray(positionHandle)  // 启用属性
        
        // 设置顶点颜色属性
        vertexBuffer?.position(COORDS_PER_VERTEX)  // 定位到颜色数据（跳过坐标）
        GLES20.glVertexAttribPointer(
            colorHandle,
            COLORS_PER_VERTEX,     // 每个颜色有 3 个分量
            GLES20.GL_FLOAT,
            false,
            stride,
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(colorHandle)
        
        // 步骤2：绘制三角形
        // glDrawArrays: 不使用索引缓冲的直接绘制
        // 参数：绘制模式、起始索引、顶点数量
        // GL_TRIANGLES: 每 3 个顶点组成一个三角形
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        
        // 步骤3：清理
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
    
    /**
     * 编译着色器
     *
     * @param type 着色器类型（GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER）
     * @param shaderCode 着色器源代码
     * @return 编译后的着色器对象 ID
     *
     * 编译流程：
     * 1. glCreateShader: 创建空着色器对象
     * 2. glShaderSource: 加载源代码
     * 3. glCompileShader: 编译
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}