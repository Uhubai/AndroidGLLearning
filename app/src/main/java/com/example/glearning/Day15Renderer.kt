/**
 * Day15Renderer: OES 外部纹理渲染器
 *
 * 本渲染器演示：
 * 1. GL_TEXTURE_EXTERNAL_OES 外部纹理
 * 2. OES_EGL_image_external 着色器扩展
 * 3. SurfaceTexture 从相机获取帧
 * 4. 纹理变换矩阵处理传感器方向
 *
 * 绘制内容：相机预览画面（全屏显示）
 *
 * 关键概念：
 * - OES 纹理：用于显示相机、视频等外部数据
 * - SurfaceTexture：连接相机输出和 OpenGL 纹理
 * - samplerExternalOES：OES 纹理专用采样器
 * - u_TextureTransform：校正 UV 坐标的变换矩阵
 */
package com.example.glearning

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Day15Renderer(
    private val cameraHelper: CameraHelper,
    private val glSurfaceView: GLSurfaceView
) : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "Day15Renderer"
        
        private const val COORDS_PER_VERTEX = 2
        private const val UV_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4
        
        /**
         * 顶点着色器：OES 外部纹理
         *
         * 关键扩展：
         * #extension GL_OES_EGL_image_external : require
         * 这是必需的，因为 OES 纹理不是标准 OpenGL ES 的一部分
         *
         * u_TextureTransform：
         * - 来自 SurfaceTexture.getTransformMatrix()
         * - 用于校正相机传感器的旋转和裁剪
         * - 不同设备有不同的传感器方向
         * - 如果不使用此矩阵，画面可能显示不正确（旋转、拉伸等）
         */
        private const val VERTEX_SHADER_CODE = """
            #extension GL_OES_EGL_image_external : require
            
            uniform mat4 u_Matrix;
            uniform mat4 u_TextureTransform;
            attribute vec4 a_Position;
            attribute vec2 a_TextureCoord;
            varying vec2 v_TextureCoord;
            
            void main() {
                gl_Position = u_Matrix * a_Position;
                // 使用变换矩阵校正 UV 坐标
                // 将标准的 [0,1] UV 坐标转换为相机实际需要的坐标
                v_TextureCoord = (u_TextureTransform * vec4(a_TextureCoord, 0.0, 1.0)).xy;
            }
        """
        
        /**
         * 片段着色器：OES 外部纹理采样
         *
         * samplerExternalOES：
         * - OES 纹理专用采样器类型
         * - 不能使用 sampler2D（会编译失败）
         * - texture2D() 函数仍可使用，但参数必须是 samplerExternalOES
         */
        private const val FRAGMENT_SHADER_CODE = """
            #extension GL_OES_EGL_image_external : require
            
            precision mediump float;
            varying vec2 v_TextureCoord;
            uniform samplerExternalOES u_Texture;
            
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TextureCoord);
            }
        """
        
        /**
         * 全屏矩形顶点数据
         *
         * 顶点坐标：归一化设备坐标（NDC），范围 [-1, 1]
         * UV 坐标：纹理坐标，范围 [0, 1]
         *
         * 数据格式：交错存储 [x, y, u, v] × 4 个顶点
         *
         * UV 原点：
         * - OpenGL UV 原点在左下角 (0, 0)
         * - 相机图像原点可能在左上角或左下角（取决于设备）
         * - u_TextureTransform 会自动处理这个差异
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
         * @param type 着色器类型（GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER）
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
    
    private var program: Int = 0
    private var positionHandle: Int = 0
    private var textureCoordHandle: Int = 0
    private var matrixHandle: Int = 0
    private var textureTransformHandle: Int = 0
    private var textureHandle: Int = 0
    
    private var vertexBuffer: FloatBuffer? = null
    private var oesTextureId: Int = 0
    private var surfaceTexture: SurfaceTexture? = null
    
    private val textureTransformMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    
    private var surfaceTextureReady: Boolean = false
    
    /**
     * 创建 OES 外部纹理
     *
     * 关键差异（与 GL_TEXTURE_2D 相比）：
     * - 使用 GLES11Ext.GL_TEXTURE_EXTERNAL_OES 而非 GLES20.GL_TEXTURE_2D
     * - OES 纹理没有固定尺寸，由外部数据（相机）决定
     * - 不能使用 glTexImage2D() 设置内容（内容由 SurfaceTexture 提供）
     *
     * @return 纹理 ID
     */
    private fun createOESTexture(): Int {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        
        val textureId = textureIds[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        
        // 设置纹理参数
        // OES 纹理只能使用 GL_LINEAR 或 GL_NEAREST
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        
        return textureId
    }
    
    /**
     * 创建 SurfaceTexture
     *
     * 重要约束：
     * - 必须在 GL 线程中调用（onSurfaceCreated 或 onDrawFrame）
     * - 必须从已创建的 OES 纹理 ID 创建
     * - SurfaceTexture 将相机帧写入 OES 纹理
     *
     * 流程：
     * 1. 创建 OES 纹理 ID
     * 2. 从纹理 ID 创建 SurfaceTexture
     * 3. 设置默认缓冲区大小（预览尺寸）
     * 4. 设置帧可用监听器
     * 5. 将 Surface 传给 CameraHelper
     */
    private fun createSurfaceTexture() {
        // 创建 OES 纹理
        oesTextureId = createOESTexture()
        if (oesTextureId == 0) {
            Log.e(TAG, "创建 OES 纹理失败")
            return
        }
        
        // 从纹理 ID 创建 SurfaceTexture
        // 这一步连接了相机输出和 OpenGL 纹理
        surfaceTexture = SurfaceTexture(oesTextureId)
        
        // 设置默认缓冲区大小
        // 预览帧将写入这个大小的缓冲区
        surfaceTexture?.setDefaultBufferSize(640, 480)
        
        // 设置帧可用监听器
        // 当相机新帧到达时，通知 GLSurfaceView 渲染
        // 注意：requestRender() 可以在非 GL 线程调用
        surfaceTexture?.setOnFrameAvailableListener {
            // 新帧到达，请求渲染
            // 这会触发 onDrawFrame() 在 GL 线程中执行
            glSurfaceView.requestRender()
        }
        
        // 将 Surface 传给 CameraHelper
        // CameraHelper 将其设置为相机预览目标
        cameraHelper.setPreviewSurface(surfaceTexture!!.surface)
        
        surfaceTextureReady = true
        Log.d(TAG, "SurfaceTexture 已创建")
    }
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // TODO: 在 Task 7 中实现
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // TODO: 在 Task 7 中实现
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // TODO: 在 Task 7 中实现
    }
}