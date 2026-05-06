/**
 * Day 7: 矩阵变换综合 - 五角星动画
 *
 * 本渲染器演示：
 * 1. 五角星顶点计算：使用三角函数将极坐标转换为直角坐标
 * 2. 矩阵变换组合：Scale → Rotate → Translate（顺序很重要）
 * 3. 索引缓冲绘制复杂图形：10 个顶点组成五角星
 * 4. 多动画效果叠加：旋转 + 缩放 + 平移同时进行
 *
 * 绘制内容：一个带顶点颜色渐变的五角星
 * - 五个尖端各有不同颜色（红、绿、蓝、紫、黄）
 * - 动画：原地旋转、周期性缩放、李萨如曲线平移
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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Day7Renderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day7Renderer"
        
        // 顶点属性常量
        private const val COORDS_PER_VERTEX = 2   // 每个顶点有 2 个坐标分量 (x, y)
        private const val COLORS_PER_VERTEX = 3   // 每个顶点有 3 个颜色分量 (r, g, b)
        private const val FLOAT_SIZE = 4          // Float 类型占用 4 字节
        private const val SHORT_SIZE = 2          // Short 类型占用 2 字节
        private const val WORLD_HALF_SIZE = 150f  // 视野半边长：覆盖五角星缩放和平移动画范围
        
        /**
         * 顶点着色器代码
         *
         * 与 Day 5 相同，接收矩阵变换并处理顶点位置和颜色
         *
         * 变量说明：
         * - uniform mat4 u_Matrix: 组合变换矩阵（Projection × Model）
         * - attribute vec4 a_Position: 顶点位置
         * - attribute vec4 a_Color: 顶点颜色
         * - varying vec4 v_Color: 传递给片段着色器的插值颜色
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
         * 五角星几何参数
         *
         * outerRadius: 外顶点到中心的距离（尖端）
         * innerRadius: 内顶点到中心的距离（凹点）
         * 比例：innerRadius ≈ outerRadius × 0.38（黄金比例相关）
         */
        private const val OUTER_RADIUS = 80f   // 外顶点半径
        private const val INNER_RADIUS = 30f   // 内顶点半径
        
        /**
         * 五角星顶点颜色
         *
         * 五个尖端各有不同颜色，GPU 会自动在五角星内部插值
         */
        private val STAR_COLORS = arrayOf(
            floatArrayOf(1.0f, 0.0f, 0.0f),  // 尖端0: 红色
            floatArrayOf(0.0f, 1.0f, 0.0f),  // 尖端1: 绿色
            floatArrayOf(0.0f, 0.0f, 1.0f),  // 尖端2: 蓝色
            floatArrayOf(1.0f, 0.0f, 1.0f),  // 尖端3: 紫色
            floatArrayOf(1.0f, 1.0f, 0.0f)   // 尖端4: 黄色
        )
        
        /**
         * 五角星顶点索引数据
         *
         * 五角星由 5 个三角形组成（每个尖端一个三角形）
         * 每个三角形由一个外顶点和两个相邻内顶点组成
         *
         * 顶点布局（交替存储）：
         * [外0, 内0, 外1, 内1, 外2, 内2, 外3, 内3, 外4, 内4]
         * 索引： 0,   1,   2,   3,   4,   5,   6,   7,   8,   9
         *
         * 三角形索引：
         * 尖端0: 外0, 内0, 内4 → [0, 1, 9]（修正：外顶点-内顶点-内顶点）
         * 实际采用简化方案：每个尖端用外顶点和相邻内顶点组成
         */
        private val STAR_INDICES = shortArrayOf(
            0, 1, 9,   // 尖端0: 外顶点0, 内顶点0, 内顶点4
            2, 3, 1,   // 尖端1: 外顶点1, 内顶点1, 内顶点0
            4, 5, 3,   // 尖端2: 外顶点2, 内顶点2, 内顶点1
            6, 7, 5,   // 尖端3: 外顶点3, 内顶点3, 内顶点2
            8, 9, 7    // 尖端4: 外顶点4, 内顶点4, 内顶点3
        )
        
        /**
         * 生成五角星顶点数据
         *
         * 使用三角函数计算顶点坐标：
         * - 极坐标 (r, θ) 转换为直角坐标 (x, y)
         * - x = r × cos(θ)
         * - y = r × sin(θ)
         *
         * @param outerRadius 外顶点半径
         * @param innerRadius 内顶点半径
         * @return 顶点数据数组 [x, y, r, g, b] × 10
         */
        private fun generateStarVertices(outerRadius: Float, innerRadius: Float): FloatArray {
            // 10 个顶点 × 5 个属性 = 50 个浮点数
            val vertices = FloatArray(10 * (COORDS_PER_VERTEX + COLORS_PER_VERTEX))
            
            for (i in 0..4) {
                // 外顶点（尖端）：角度 = i × 72°
                val outerAngleRad = i * 72f * (PI / 180f)
                val outerX = outerRadius * cos(outerAngleRad).toFloat()
                val outerY = outerRadius * sin(outerAngleRad).toFloat()
                
                // 内顶点（凹点）：角度 = i × 72° + 36°（偏移半个间隔）
                val innerAngleRad = (i * 72f + 36f) * (PI / 180f)
                val innerX = innerRadius * cos(innerAngleRad).toFloat()
                val innerY = innerRadius * sin(innerAngleRad).toFloat()
                
                // 外顶点索引：i × 2（交替存储）
                val outerIndex = i * 2 * (COORDS_PER_VERTEX + COLORS_PER_VERTEX)
                vertices[outerIndex] = outerX
                vertices[outerIndex + 1] = outerY
                // 外顶点颜色：使用该尖端的颜色
                vertices[outerIndex + 2] = STAR_COLORS[i][0]
                vertices[outerIndex + 3] = STAR_COLORS[i][1]
                vertices[outerIndex + 4] = STAR_COLORS[i][2]
                
                // 内顶点索引：i × 2 + 1
                val innerIndex = (i * 2 + 1) * (COORDS_PER_VERTEX + COLORS_PER_VERTEX)
                vertices[innerIndex] = innerX
                vertices[innerIndex + 1] = innerY
                // 内顶点颜色：混合相邻两个尖端的颜色
                val nextI = (i + 1) % 5
                vertices[innerIndex + 2] = (STAR_COLORS[i][0] + STAR_COLORS[nextI][0]) / 2
                vertices[innerIndex + 3] = (STAR_COLORS[i][1] + STAR_COLORS[nextI][1]) / 2
                vertices[innerIndex + 4] = (STAR_COLORS[i][2] + STAR_COLORS[nextI][2]) / 2
            }
            
            return vertices
        }
    }
    
    // 着色器程序相关
    private var program: Int = 0              // 着色器程序 ID
    private var positionHandle: Int = 0       // 顶点位置属性句柄
    private var colorHandle: Int = 0          // 顶点颜色属性句柄
    private var matrixHandle: Int = 0         // 变换矩阵 uniform 句柄
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null   // 顶点坐标和颜色缓冲区
    private var indexBuffer: ShortBuffer? = null    // 顶点索引缓冲区
    
    // 矩阵相关（4×4 矩阵 = 16 个浮点数）
    private val projectionMatrix = FloatArray(16)   // 投影矩阵
    private val modelMatrix = FloatArray(16)        // 模型矩阵（缩放+旋转+平移）
    private val resultMatrix = FloatArray(16)       // 结果矩阵（投影×模型）
    
    // 动画相关
    private var startTime: Long = 0                 // 动画开始时间
    
    /**
     * Surface 创建时的初始化回调
     *
     * 执行时机：GLSurfaceView 创建或重建时调用一次
     * 主要任务：
     * 1. 设置清屏颜色
     * 2. 创建并编译着色器
     * 3. 创建着色器程序
     * 4. 获取属性和 uniform 句柄
     * 5. 生成五角星顶点数据
     * 6. 创建顶点和索引缓冲区
     */
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置清屏颜色为黑色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 记录动画开始时间
        startTime = System.currentTimeMillis()
        
        // 步骤1：编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        // 步骤2：创建着色器程序并链接
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 步骤3：获取属性和 uniform 句柄
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        colorHandle = GLES20.glGetAttribLocation(program, "a_Color")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        
        // 步骤4：生成五角星顶点数据
        val starVertices = generateStarVertices(OUTER_RADIUS, INNER_RADIUS)
        
        // 步骤5：创建顶点数据缓冲区
        val vb = ByteBuffer.allocateDirect(starVertices.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(starVertices)
        vertexBuffer?.position(0)
        
        // 步骤6：创建索引数据缓冲区
        val ib = ByteBuffer.allocateDirect(STAR_INDICES.size * SHORT_SIZE)
        ib.order(ByteOrder.nativeOrder())
        indexBuffer = ib.asShortBuffer()
        indexBuffer?.put(STAR_INDICES)
        indexBuffer?.position(0)
    }
    
    /**
     * Surface 尺寸变化时的回调
     *
     * 执行时机：Surface 创建后、屏幕旋转时调用
     * 主要任务：
     * 1. 设置视口大小
     * 2. 创建正交投影矩阵（屏幕适配）
     */
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口覆盖整个 Surface
        GLES20.glViewport(0, 0, width, height)
        
        // 计算宽高比，用于保持图形比例不变形
        val aspectRatio = width.toFloat() / height.toFloat()
        
        // 创建正交投影矩阵（屏幕适配）
        // 关键：投影边界必须与模型坐标同量级，否则图形会被裁剪
        // 本例五角星外半径 80，且存在缩放+平移，因此使用 ±150 作为基础可视范围
        if (aspectRatio > 1f) {
            // 橫屏模式：扩展左右范围
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio * WORLD_HALF_SIZE, aspectRatio * WORLD_HALF_SIZE,
                -WORLD_HALF_SIZE, WORLD_HALF_SIZE,
                -1f, 1f
            )
        } else {
            // 竖屏模式：扩展上下范围
            android.opengl.Matrix.orthoM(
                projectionMatrix, 0,
                -WORLD_HALF_SIZE, WORLD_HALF_SIZE,
                -WORLD_HALF_SIZE / aspectRatio, WORLD_HALF_SIZE / aspectRatio,
                -1f, 1f
            )
        }
    }
    
    /**
     * 每帧绘制回调
     *
     * 执行时机：每秒调用约 60 次
     * 主要任务：
     * 1. 清除屏幕
     * 2. 计算动画参数（旋转角度、缩放比例、平移距离）
     * 3. 组合矩阵变换（缩放 → 旋转 → 平移）
     * 4. 设置着色器参数
     * 5. 绘制五角星
     *
     * 矩阵变换顺序：
     * - 矩阵乘法从右到左执行
     * - 先缩放：改变物体大小
     * - 再旋转：绕中心旋转
     * - 最后平移：移动位置
     *
     * 结果：gl_Position = Projection × Translation × Rotation × Scale × Vertex
     */
    override fun onDrawFrame(gl: GL10?) {
        // 清除颜色缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 计算动画时间
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - startTime) / 1000f
        
        // 步骤1：计算动画参数
        // 旋转：每秒 60 度，持续旋转
        val angle = elapsedSeconds * 60f
        
        // 缩放：周期性变化，范围 [0.7, 1.3]
        // sin(x) 范围 [-1, 1]，乘以 0.3 并加上 1.0 得到 [0.7, 1.3]
        val scale = 1.0f + sin(elapsedSeconds * 2.0).toFloat() * 0.3f
        
        // 平移：李萨如曲线轨迹
        // 不同频率产生复杂轨迹
        val tx = sin(elapsedSeconds * 1.5).toFloat() * 50f
        val ty = cos(elapsedSeconds * 2.0).toFloat() * 30f
        
        // 步骤2：组合矩阵变换
        // 顺序：先缩放 → 再旋转 → 最后平移
        // 矩阵乘法从右到左执行，所以代码顺序是反向的
        
        // 重置模型矩阵为单位矩阵
        android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        
        // 先缩放（最后执行）
        // scaleM(matrix, offset, sx, sy, sz)
        android.opengl.Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)
        
        // 再旋转（中间执行）
        // rotateM(matrix, offset, angle, axisX, axisY, axisZ)
        // 绕 Z 轴旋转（0, 0, 1）
        android.opengl.Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)
        
        // 最后平移（最先执行）
        // translateM(matrix, offset, tx, ty, tz)
        android.opengl.Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
        
        // 步骤3：组合投影矩阵和模型矩阵
        // multiplyMM(result, 0, left, 0, right, 0) 计算 left × right
        android.opengl.Matrix.multiplyMM(
            resultMatrix, 0,
            projectionMatrix, 0,
            modelMatrix, 0
        )
        
        // 步骤4：激活着色器程序
        GLES20.glUseProgram(program)
        
        // 步骤5：传递变换矩阵给着色器
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
        
        // 步骤6：设置顶点属性指针
        val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE
        
        // 顶点位置
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
        
        // 顶点颜色
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
        
        // 步骤7：绘制五角星
        // glDrawElements：使用索引绘制
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            STAR_INDICES.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )
        
        // 步骤8：清理
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
    
    /**
     * 编译着色器
     *
     * @param type 着色器类型（GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER）
     * @param shaderCode 着色器源代码
     * @return 编译后的着色器对象 ID
     */
    private fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
}