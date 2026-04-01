/**
 * Day 5: 平移矩阵 + 动画基础
 *
 * 本渲染器演示：
 * 1. 齐次坐标概念 - 用 4D 向量表示 2D/3D 点，统一变换操作
 * 2. 平移矩阵原理 - 通过矩阵乘法实现物体位移
 * 3. 矩阵乘法顺序 - Projection × Model × Vertex
 * 4. 基于时间的动画 - 使用 sin/cos 创建周期性运动
 *
 * 绘制内容：一个带顶点颜色渐变的矩形，沿李萨如曲线轨迹移动
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

class Day5Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day5Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val COLORS_PER_VERTEX = 3   // 每个顶点有 3 个颜色分量 (r, g, b)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        private const val SHORT_SIZE = 2          // Short 类型占用 2 字节
        
        /**
         * 顶点着色器代码
         *
         * 关键点：
         * - uniform mat4 u_Matrix: 从外部传入的变换矩阵（投影×模型）
         * - attribute vec4 a_Position: 顶点位置属性，每个顶点不同
         * - attribute vec4 a_Color: 顶点颜色属性，每个顶点不同
         * - varying vec4 v_Color: 传递给片段着色器的插值颜色
         *
         * 工作流程：
         * 1. 接收顶点位置和颜色
         * 2. 应用变换矩阵：gl_Position = u_Matrix * a_Position
         * 3. 将颜色传递给片段着色器：v_Color = a_Color
         */
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_Matrix;
            attribute vec4 a_Position;
            attribute vec4 a_Color;
            varying vec4 v_Color;
            
            void main() {
                gl_Position = u_Matrix * a_Position;
                v_Color = a_Color;
            }
        """
        
        /**
         * 片段着色器代码
         *
         * 关键点：
         * - precision mediump float: 设置浮点精度为中等（性能与质量平衡）
         * - varying vec4 v_Color: 从顶点着色器接收的插值颜色
         *
         * 工作流程：
         * 1. 接收插值后的颜色（GPU 自动在三角形内插值）
         * 2. 直接输出为最终像素颜色
         */
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec4 v_Color;
            
            void main() {
                gl_FragColor = v_Color;
            }
        """
        
        /**
         * 矩形的顶点坐标和颜色数据
         *
         * 数据结构：交错存储（坐标+颜色）
         * [x, y, r, g, b] × 4 个顶点 = 20 个浮点数
         *
         * 顶点布局：
         *   顶点0 (-100, 100) 红色 (1, 0, 0) ──── 顶点2 (100, 100) 蓝色 (0, 0, 1)
         *   │                                    │
         *   │                                    │
         *   顶点1 (-100, -100) 绿色 (0, 1, 0) ──── 顶点3 (100, -100) 黄色 (1, 1, 0)
         *
         * 坐标范围：-100 到 100，矩形中心在原点
         */
        private val RECT_COORDS_AND_COLORS = floatArrayOf(
            -100f,  100f,  1.0f, 0.0f, 0.0f,  // 顶点0: 左上角，红色
            -100f, -100f,  0.0f, 1.0f, 0.0f,  // 顶点1: 左下角，绿色
             100f,  100f,  0.0f, 0.0f, 1.0f,  // 顶点2: 右上角，蓝色
             100f, -100f,  1.0f, 1.0f, 0.0f   // 顶点3: 右下角，黄色
        )
        
        /**
         * 矩形的顶点索引数据
         *
         * 使用索引绘制的好处：
         * - 减少数据重复：矩形只需 4 个顶点而非 6 个
         * - 提高渲染效率：GPU 缓存顶点数据
         *
         * 两个三角形组成一个矩形：
         * - 三角形1: 顶点 0, 1, 2（左上、左下、右上）
         * - 三角形2: 顶点 1, 3, 2（左下、右下、右上）
         *
         * 注意：顶点顺序遵循逆时针方向（正面）
         */
        private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性的位置句柄
    private var colorHandle: Int = 0          // 顶点颜色属性的位置句柄
    private var matrixHandle: Int = 0         // 变换矩阵的 uniform 句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和颜色数据缓冲区
    private var indexBuffer: ShortBuffer? = null    // 顶点索引数据缓冲区
    
    // 矩阵相关（OpenGL 使用 4×4 矩阵，即 16 个浮点数）
    private val projectionMatrix = FloatArray(16)   // 投影矩阵：定义可视范围
    private val modelMatrix = FloatArray(16)        // 模型矩阵：物体自身变换（平移、旋转、缩放）
    private val resultMatrix = FloatArray(16)       // 结果矩阵：投影×模型的组合矩阵
    
    // 动画相关
    private var startTime: Long = 0                 // 动画开始时间（用于计算时间差）
    
    /**
     * Surface 创建时的初始化回调
     *
     * 执行时机：GLSurfaceView 创建或重建时调用一次
     * 主要任务：
     * 1. 设置清屏颜色
     * 2. 创建并编译着色器
     * 3. 创建着色器程序
     * 4. 获取属性和 uniform 句柄
     * 5. 创建顶点数据缓冲区
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色为黑色（RGBA: 0, 0, 0, 1）
        // 参数范围：0.0f ~ 1.0f
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 记录动画开始时间，用于后续计算动画进度
        startTime = System.currentTimeMillis()
        
        // 步骤1：编译顶点着色器和片段着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        // 步骤2：创建着色器程序并链接
        program = GLES20.glCreateProgram()          // 创建空程序对象
        GLES20.glAttachShader(program, vertexShader)   // 附着顶点着色器
        GLES20.glAttachShader(program, fragmentShader) // 附着片段着色器
        GLES20.glLinkProgram(program)                  // 链接程序（编译+链接）
        
        // 步骤3：获取着色器中的属性和 uniform 句柄
        // 属性（attribute）：每个顶点不同的数据，如位置、颜色
        // Uniform：所有顶点共享的数据，如变换矩阵
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(program, "a_Color")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        
        // 步骤4：创建顶点数据缓冲区
        // 使用 ByteBuffer.allocateDirect 创建直接内存缓冲区
        // 直接内存：避免 JVM 堆到本地内存的拷贝，提高性能
        
        // 创建顶点坐标和颜色缓冲区
        val vb = ByteBuffer.allocateDirect(RECT_COORDS_AND_COLORS.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())            // 设置字节序为本地字节序（提高效率）
        vertexBuffer = vb.asFloatBuffer()            // 转换为 FloatBuffer
        vertexBuffer?.put(RECT_COORDS_AND_COLORS)    // 写入顶点数据
        vertexBuffer?.position(0)                    // 重置位置指针到开头
        
        // 创建顶点索引缓冲区
        val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(RECT_INDICES)
        indexBuffer?.position(0)
    }
    
    /**
     * Surface 尺寸变化时的回调
     *
     * 执行时机：Surface 创建后、屏幕旋转、窗口大小改变时调用
     * 主要任务：
     * 1. 设置视口大小
     * 2. 创建正交投影矩阵（适配不同屏幕比例）
     *
     * 正交投影（Orthographic Projection）：
     * - 无透视效果，物体大小不随距离变化
     * - 适合 2D 图形和 UI 渲染
     * - 将世界坐标映射到 [-1, 1] 的标准化设备坐标
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口：定义渲染区域在屏幕上的位置和大小
        GLES20.glViewport(0, 0, width, height)
        
        // 计算屏幕宽高比，用于保持图形比例不变形
        val aspectRatio = width.toFloat() / height.toFloat()
        
        // 根据宽高比创建正交投影矩阵
        // 目标：无论屏幕宽窄，图形都能正确显示不变形
        
        if (aspectRatio > 1f) {
            // 横屏模式：宽度大于高度
            // 扩展左右范围，保持上下为 [-1, 1]
            // 例如：宽高比 16:9 = 1.78，则左右范围为 [-1.78, 1.78]
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio, aspectRatio,  // 左右边界
                -1f, 1f,                    // 上下边界
                -1f, 1f                     // 近远边界
            )
        } else {
            // 竖屏模式：高度大于宽度
            // 扩展上下范围，保持左右为 [-1, 1]
            // 例如：宽高比 9:16 = 0.56，则上下范围为 [-1.78, 1.78]
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -1f, 1f,                        // 左右边界
                -1f / aspectRatio, 1f / aspectRatio,  // 上下边界
                -1f, 1f                         // 近远边界
            )
        }
    }
    
    /**
     * 每帧绘制回调
     *
     * 执行时机：每秒调用约 60 次（与屏幕刷新率同步）
     * 主要任务：
     * 1. 清除屏幕
     * 2. 计算动画时间
     * 3. 更新模型矩阵（平移）
     * 4. 组合投影矩阵和模型矩阵
     * 5. 设置着色器参数
     * 6. 绘制矩形
     *
     * 动画原理：
     * - 使用 sin/cos 三角函数创建周期性运动
     * - sin(x): 范围 [-1, 1]，周期 2π，用于水平移动
     * - cos(x): 范围 [-1, 1]，周期 2π，用于垂直移动
     * - 不同频率（2.0 和 3.0）产生李萨如曲线轨迹
     */
    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色缓冲区，用之前设置的清屏颜色填充整个屏幕
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 计算动画时间：从开始到现在经过的秒数
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - startTime) / 1000f
        
        // 计算平移偏移量（李萨如曲线）
        // 李萨如曲线：两个不同频率的简谐运动的组合
        // tx: 水平方向，频率 2.0，幅度 100（左右移动范围）
        // ty: 垂直方向，频率 3.0，幅度 50（上下移动范围）
        // 频率比 2:3 产生封闭的曲线图案
        val tx = Math.sin(elapsedSeconds * 2.0).toFloat() * 100f
        val ty = Math.cos(elapsedSeconds * 3.0).toFloat() * 50f
        
        // 步骤1：重置模型矩阵为单位矩阵
        // 单位矩阵：对角线为 1，其余为 0，相当于"无变换"
        // 每帧必须重置，否则变换会累积
        android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        
        // 步骤2：应用平移变换
        // 参数：目标矩阵、偏移量起始索引、x偏移、y偏移、z偏移
        // 这里只做 2D 平移，z 方向设为 0
        android.opengl.Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
        
        // 步骤3：组合投影矩阵和模型矩阵
        // 矩阵乘法顺序：Projection × Model × Vertex
        // - Model 变换先执行：将物体从模型空间转换到世界空间
        // - Projection 变换后执行：将世界空间转换到标准化设备坐标
        // multiplyMM(result, 0, left, 0, right, 0) 计算 left × right
        android.opengl.Matrix.multiplyMM(
            resultMatrix, 0,
            projectionMatrix, 0,
            modelMatrix, 0
        )
        
        // 步骤4：激活着色器程序
        GLES20.glUseProgram(program)
        
        // 步骤5：传递变换矩阵给着色器
        // glUniformMatrix4fv：传递 4×4 矩阵数据
        // 参数：句柄、矩阵数量、是否转置（OpenGL ES 要求 false）、矩阵数据、起始偏移
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        
        // 步骤6：设置顶点属性指针
        // stride（步幅）：一个顶点数据占用的总字节数
        // 这里每个顶点有 2 个坐标 + 3 个颜色 = 5 个浮点数
        val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE
        
        // 设置顶点位置属性
        // 交错数据格式：[x, y, r, g, b, x, y, r, g, b, ...]
        // 需要指定步幅让 GPU 跳过颜色数据找到下一个顶点的位置
        vertexBuffer?.position(0)  // 定位到第一个顶点的位置数据开头
        GLES20.glVertexAttribPointer(
            positionHandle,
            COORDS_PER_VERTEX,        // 每个位置有 2 个分量 (x, y)
            GLES20.GL_FLOAT,          // 数据类型为浮点
            false,                    // 不归一化（浮点数据不需要）
            stride,                   // 步幅：跳过整个顶点数据
            vertexBuffer              // 数据源
        )
        GLES20.glEnableVertexAttribArray(positionHandle)  // 启用位置属性
        
        // 设置顶点颜色属性
        // 定位到颜色数据：跳过 2 个坐标分量
        vertexBuffer?.position(COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(
            colorHandle,
            COLORS_PER_VERTEX,        // 每个颜色有 3 个分量 (r, g, b)
            GLES20.GL_FLOAT,
            false,
            stride,                   // 相同步幅
            vertexBuffer
        )
        GLES20.glEnableVertexAttribArray(colorHandle)  // 启用颜色属性
        
        // 步骤7：绘制矩形
        // glDrawElements：使用索引绘制
        // 参数：绘制模式、索引数量、索引数据类型、索引缓冲区
        // GL_TRIANGLES：每 3 个索引组成一个三角形
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            RECT_INDICES.size,        // 6 个索引 = 2 个三角形
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        
        // 步骤8：清理：禁用顶点属性数组
        // 良好习惯：绘制完成后禁用属性，避免影响后续绘制
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
    
    /**
     * 编译着色器
     *
     * @param type 着色器类型
     *             - GLES20.GL_VERTEX_SHADER: 顶点着色器
     *             - GLES20.GL_FRAGMENT_SHADER: 片段着色器
     * @param shaderCode 着色器源代码字符串
     * @return 编译后的着色器对象 ID
     *
     * 编译流程：
     * 1. glCreateShader：创建空着色器对象
     * 2. glShaderSource：加载着色器源代码
     * 3. glCompileShader：编译着色器
     *
     * 注意：实际应用中应该检查编译状态和错误信息
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        // 创建着色器对象
        val shader = GLES20.glCreateShader(type)
        
        // 加载着色器源代码
        GLES20.glShaderSource(shader, shaderCode)
        
        // 编译着色器
        GLES20.glCompileShader(shader)
        
        return shader
    }
}