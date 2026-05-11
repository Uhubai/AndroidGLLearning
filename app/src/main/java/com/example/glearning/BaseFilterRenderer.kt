/**
 * BaseFilterRenderer: 滤镜基类
 *
 * 本抽象类演示：
 * 1. 统一顶点着色器（MVP + 纹理坐标）
 * 2. 完整渲染管线封装（着色器加载、程序链接、uniform 句柄获取）
 * 3. 双模式纹理支持（程序化纹理 + OES 相机预览）
 * 4. 参数缓存和更新机制（减少不必要的 GL 调用）
 * 5. 正交投影矩阵标准实现
 *
 * 绘制内容：全屏矩形，应用滤镜效果
 *
 * 关键概念：
 * - 抽象类：子类只需提供片段着色器和 uniform 参数
 * - 双模式：程序化纹理用于学习，相机预览用于实战
 * - 参数缓存：只在值变化时更新 uniform，减少 GL 调用开销
 * - 完整错误检查：着色器编译和链接都有验证
 *
 * 子类职责：
 * - 实现 getFragmentShader(): 返回片段着色器代码
 * - 实现 setupUniforms(program): 设置滤镜特定的 uniform 句柄
 * - 在 onDrawFrame 中调用 updateUniformIfNeeded() 更新参数
 */
package com.example.glearning

import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

abstract class BaseFilterRenderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "BaseFilterRenderer"
        
        /**
         * 世界坐标基准半边长
         *
         * 全屏矩形使用 NDC 坐标 [-1, 1]，但为了统一正交投影规范，
         * 仍定义此常量以保持一致性。
         */
        private const val WORLD_HALF_SIZE = 1f
        
        private const val COORDS_PER_VERTEX = 2
        private const val UV_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4
        
        /**
         * 统一顶点着色器代码
         *
         * 关键变量：
         * - u_Matrix: MVP 变换矩阵（uniform）
         * - a_Position: 顶点位置（attribute）
         * - a_TextureCoord: 纹理坐标（attribute）
         * - v_TextureCoord: 传递给片段着色器的纹理坐标（varying）
         *
         * 工作流程：
         * 1. 顶点位置乘以 MVP 矩阵得到裁剪坐标
         * 2. 纹理坐标直接传递给片段着色器
         */
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_Matrix;
            attribute vec4 a_Position;
            attribute vec2 a_TextureCoord;
            varying vec2 v_TextureCoord;
            
            void main() {
                gl_Position = u_Matrix * a_Position;
                v_TextureCoord = a_TextureCoord;
            }
        """
        
        /**
         * 全屏矩形顶点数据
         *
         * 数据格式：交错存储 [x, y, u, v] × 4 个顶点
         *
         * 顶点坐标：归一化设备坐标（NDC），范围 [-1, 1]
         * UV 坐标：纹理坐标，范围 [0, 1]
         *
         * 顶点布局图示：
         *   左上 (-1, 1) ──── 右上 (1, 1)
         *   │  UV:(0, 1)      │  UV:(1, 1)
         *   │                 │
         *   左下 (-1, -1) ──── 右下 (1, -1)
         *   │  UV:(0, 0)      │  UV:(1, 0)
         */
        private val FULL_SCREEN_RECT = floatArrayOf(
            -1.0f,  1.0f,  0.0f, 1.0f,  // 左上
            -1.0f, -1.0f,  0.0f, 0.0f,  // 左下
             1.0f,  1.0f,  1.0f, 1.0f,  // 右上
             1.0f, -1.0f,  1.0f, 0.0f   // 右下
        )
        
        /**
         * 加载着色器
         *
         * @param type 着色器类型（GLES20.GL_VERTEX_SHADER 或 GLES20.GL_FRAGMENT_SHADER）
         * @param shaderCode 着色器源代码
         * @return 着色器 ID，失败返回 0
         */
        private fun loadShader(type: Int, shaderCode: String): Int {
            val shader = GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
            
            // 检查编译状态
            val compiled = IntArray(1)
            GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
            if (compiled[0] == GLES20.GL_FALSE) {
                Log.e(TAG, "着色器编译失败: ${GLES20.glGetShaderInfoLog(shader)}")
                GLES20.glDeleteShader(shader)
                return 0
            }
            
            return shader
        }
    }
    
    // 着色器程序相关
    protected var program: Int = 0
    protected var positionHandle: Int = 0
    protected var textureCoordHandle: Int = 0
    protected var matrixHandle: Int = 0
    protected var textureHandle: Int = 0
    
    // 顶点数据缓冲区
    private var vertexBuffer: FloatBuffer? = null
    
    // 纹理相关
    private var textureId: IntArray = IntArray(1)
    private var oesTextureId: IntArray = IntArray(1)
    private var cameraHelper: CameraHelper? = null
    private var surfaceTexture: SurfaceTexture? = null
    private var surfaceTextureReady: Boolean = false
    
    /**
     * 纹理模式标志
     *
     * false = 程序化纹理（默认，用于学习）
     * true = OES 相机预览（用于实战）
     */
    var useCameraTexture: Boolean = false
    
    // MVP 矩阵相关
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val textureTransformMatrix = FloatArray(16)
    
    // 动画相关
    private var startTime: Long = 0
    
    /**
     * 获取片段着色器代码
     *
     * 子类必须实现此方法，返回滤镜特定的片段着色器代码。
     *
     * @return 片段着色器代码字符串
     */
    abstract fun getFragmentShader(): String
    
    /**
     * 设置滤镜特定的 uniform 句柄
     *
     * 子类在此方法中获取自定义 uniform 的位置，
     * 例如亮度、对比度、饱和度等参数的句柄。
     *
     * @param program 着色器程序 ID
     */
    abstract fun setupUniforms(program: Int)
    
    /**
     * 创建程序化纹理
     *
     * 创建一个 256x256 的渐变纹理，用于学习滤镜算法。
     * 渐变从红色到绿色到蓝色，便于观察滤镜对颜色的影响。
     *
     * @return 纹理 ID
     */
    private fun createProceduralTexture(): Int {
        val size = 256
        val colors = IntArray(size * size)
        
        // 创建彩虹渐变纹理
        for (y in 0 until size) {
            for (x in 0 until size) {
                val r = (x * 255 / size).toByte()
                val g = (y * 255 / size).toByte()
                val b = ((x + y) * 255 / (size * 2)).toByte()
                colors[y * size + x] = 0xFF000000.toInt() or 
                    ((r.toInt() and 0xFF) shl 16) or 
                    ((g.toInt() and 0xFF) shl 8) or 
                    (b.toInt() and 0xFF)
            }
        }
        
        val bitmap = Bitmap.createBitmap(colors, size, size, Bitmap.Config.ARGB_8888)
        val textureId = createTexture(bitmap)
        bitmap.recycle()
        
        return textureId
    }
    
    /**
     * 创建 2D 纹理
     *
     * @param bitmap 位图数据
     * @return 纹理 ID
     */
    private fun createTexture(bitmap: Bitmap): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        
        val textureId = textureIds[0]
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        
        // 设置纹理参数
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        // 加载位图数据到纹理
        android.opengl.GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        
        // 解绑纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        
        return textureId
    }
    
    /**
     * 创建 OES 外部纹理
     *
     * OES 纹理用于显示相机预览等外部数据源。
     * 与 GL_TEXTURE_2D 的差异：
     * - 使用 GLES11Ext.GL_TEXTURE_EXTERNAL_OES
     * - 不能手动设置内容（由 SurfaceTexture 提供）
     * - 着色器中必须使用 samplerExternalOES 采样器
     *
     * @return 纹理 ID
     */
    private fun createOESTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        
        val textureId = textureIds[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        
        // OES 纹理参数设置
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        
        return textureId
    }
    
    /**
     * 设置 OES 纹理和 SurfaceTexture
     *
     * 此方法连接相机输出和 OpenGL 纹理：
     * 1. 创建 OES 纹理 ID
     * 2. 从纹理 ID 创建 SurfaceTexture
     * 3. 设置帧可用监听器
     * 4. 将 Surface 传给 CameraHelper
     */
    private fun setupOESTexture(glSurfaceView: GLSurfaceView) {
        // 创建 OES 纹理
        oesTextureId[0] = createOESTexture()
        if (oesTextureId[0] == 0) {
            Log.e(TAG, "创建 OES 纹理失败")
            return
        }
        
        // 从纹理 ID 创建 SurfaceTexture
        surfaceTexture = SurfaceTexture(oesTextureId[0])
        
        // 设置默认缓冲区大小
        surfaceTexture?.setDefaultBufferSize(640, 480)
        
        // 设置帧可用监听器
        surfaceTexture?.setOnFrameAvailableListener {
            glSurfaceView.requestRender()
        }
        
        // 将 Surface 传给 CameraHelper
        cameraHelper?.let {
            it.setPreviewSurface(Surface(surfaceTexture!!))
        }
        
        surfaceTextureReady = true
        Log.d(TAG, "OES 纹理和 SurfaceTexture 已创建")
    }
    
    /**
     * 切换纹理模式
     *
     * @param useCamera true = 相机预览，false = 程序化纹理
     */
    fun switchTextureMode(useCamera: Boolean) {
        useCameraTexture = useCamera
        Log.d(TAG, "纹理模式切换: ${if (useCamera) "相机预览" else "程序化纹理"}")
    }
    
    /**
     * 参数缓存和更新机制
     *
     * 只在参数值变化超过阈值时才调用 glUniform，
     * 减少不必要的 GL 调用开销。
     *
     * @param handle uniform 句柄
     * @param newValue 新值
     * @param oldValue 旧值
     * @return 实际使用的值（如果更新则返回 newValue，否则返回 oldValue）
     */
    protected fun updateUniformIfNeeded(
        handle: Int,
        newValue: Float,
        oldValue: Float
    ): Float {
        if (kotlin.math.abs(newValue - oldValue) > 0.001f) {
            GLES20.glUniform1f(handle, newValue)
            return newValue
        }
        return oldValue
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 步骤 1：设置背景色
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        
        // 步骤 2：初始化动画时间
        startTime = System.currentTimeMillis()
        
        // 步骤 3：编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, getFragmentShader())
        
        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "着色器编译失败")
            return
        }
        
        // 步骤 4：创建着色器程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 步骤 5：检查链接状态
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == GLES20.GL_FALSE) {
            Log.e(TAG, "着色器程序链接失败: ${GLES20.glGetProgramInfoLog(program)}")
            return
        }
        
        // 步骤 6：获取 attribute 和 uniform 的位置
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        textureCoordHandle = GLES20.glGetAttribLocation(program, "a_TextureCoord")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")
        
        // 步骤 7：调用子类方法设置特定 uniform
        setupUniforms(program)
        
        // 步骤 8：创建顶点缓冲区
        val vb = ByteBuffer.allocateDirect(FULL_SCREEN_RECT.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(FULL_SCREEN_RECT)
        vertexBuffer?.position(0)
        
        // 步骤 9：根据模式创建纹理
        if (useCameraTexture) {
            setupOESTexture(glSurfaceView = glSurfaceView as GLSurfaceView)
        } else {
            textureId[0] = createProceduralTexture()
        }
        
        // 步骤 10：初始化矩阵
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(textureTransformMatrix, 0)
        
        Log.d(TAG, "BaseFilterRenderer 初始化完成")
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 步骤 1：设置视口
        GLES20.glViewport(0, 0, width, height)
        
        // 步骤 2：计算宽高比
        val aspectRatio = width.toFloat() / height.toFloat()
        
        // 步骤 3：设置正交投影矩阵
        // 使用 WORLD_HALF_SIZE = 1f（NDC 坐标范围）
        if (aspectRatio > 1f) {
            // 横屏：扩展水平范围
            Matrix.orthoM(
                projectionMatrix, 0,
                -aspectRatio * WORLD_HALF_SIZE, aspectRatio * WORLD_HALF_SIZE,
                -WORLD_HALF_SIZE, WORLD_HALF_SIZE,
                -1f, 1f
            )
        } else {
            // 竖屏：扩展垂直范围
            Matrix.orthoM(
                projectionMatrix, 0,
                -WORLD_HALF_SIZE, WORLD_HALF_SIZE,
                -WORLD_HALF_SIZE / aspectRatio, WORLD_HALF_SIZE / aspectRatio,
                -1f, 1f
            )
        }
        
        Log.d(TAG, "视口设置完成: width=$width, height=$height")
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // 步骤 1：清除缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 步骤 2：如果使用相机纹理，更新纹理内容
        if (useCameraTexture) {
            if (!surfaceTextureReady || surfaceTexture == null) {
                return
            }
            surfaceTexture?.updateTexImage()
            surfaceTexture?.getTransformMatrix(textureTransformMatrix)
        }
        
        // 步骤 3：使用着色器程序
        GLES20.glUseProgram(program)
        
        // 步骤 4：设置 MVP 矩阵（全屏显示，使用单位矩阵）
        Matrix.setIdentityM(modelMatrix, 0)
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, projectionMatrix, 0)
        
        // 步骤 5：绑定纹理
        if (useCameraTexture) {
            // OES 相机纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId[0])
            GLES20.glUniform1i(textureHandle, 0)
        } else {
            // 程序化纹理
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
            GLES20.glUniform1i(textureHandle, 0)
        }
        
        // 步骤 6：绘制全屏矩形
        drawFullScreenRect()
        
        // 步骤 7：解绑纹理
        if (useCameraTexture) {
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        } else {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }
    }
    
    /**
     * 绘制全屏矩形
     *
     * 使用 GL_TRIANGLE_STRIP 绘制 4 个顶点。
     * 绘制顺序：0-1-2-3（左上-左下-右上-右下）
     */
    private fun drawFullScreenRect() {
        val stride = (COORDS_PER_VERTEX + UV_PER_VERTEX) * FLOAT_SIZE
        
        // 设置顶点位置
        vertexBuffer?.position(0)
        GLES20.glVertexAttribPointer(positionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(positionHandle)
        
        // 设置纹理坐标
        vertexBuffer?.position(COORDS_PER_VERTEX)
        GLES20.glVertexAttribPointer(textureCoordHandle, UV_PER_VERTEX, GLES20.GL_FLOAT, false, stride, vertexBuffer)
        GLES20.glEnableVertexAttribArray(textureCoordHandle)
        
        // 绘制矩形（4 个顶点）
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        
        // 清理：禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureCoordHandle)
    }
}
