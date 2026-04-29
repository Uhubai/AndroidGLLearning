# Day 15 实现计划：SurfaceTexture + OES 外部纹理

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现相机预览到 GLSurfaceView，使用 Camera2 API + OES 外部纹理

**Architecture:** Renderer 创建 OES 纹理和 SurfaceTexture，将 Surface 传给 CameraHelper 作为相机预览目标，相机帧通过 SurfaceTexture 更新纹理内容

**Tech Stack:** Android Camera2 API, OpenGL ES 2.0 OES 扩展, SurfaceTexture

---

## 文件结构

| 文件 | 类型 | 职责 |
|------|------|------|
| `app/src/main/java/com/example/glearning/CameraHelper.kt` | 创建 | Camera2 预览封装，接收 Surface |
| `app/src/main/java/com/example/glearning/Day15Renderer.kt` | 创建 | OES 纹理渲染，创建 SurfaceTexture |
| `app/src/main/java/com/example/glearning/MainActivity.kt` | 修改 | 权限请求 + 生命周期管理 |
| `app/src/main/AndroidManifest.xml` | 修改 | 添加 CAMERA 权限 |

---

## Chunk 1: 基础配置

### Task 1: 添加相机权限

**Files:**
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: 在 AndroidManifest.xml 中添加相机权限**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- 相机权限 -->
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />

    <application
        android:allowBackup="true"
        android:icon="@drawable/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@drawable/ic_launcher"
        android:supportsRtl="true"
        android:theme="@style/Theme.GLearning">
        
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

- [ ] **Step 2: 提交更改**

```bash
git add app/src/main/AndroidManifest.xml
git commit -m "feat(day15): 添加相机权限声明"
```

---

## Chunk 2: CameraHelper 类

### Task 2: 创建 CameraHelper 类骨架

**Files:**
- Create: `app/src/main/java/com/example/glearning/CameraHelper.kt`

- [ ] **Step 1: 创建 CameraHelper 类基础结构**

```kotlin
/**
 * CameraHelper: Camera2 预览封装类
 *
 * 职责：
 * - 接收来自 Renderer 的 Surface 作为相机预览目标
 * - 管理 CameraDevice 和 CaptureSession
 * - 启动和停止相机预览
 *
 * 重要约束：
 * - 不创建 SurfaceTexture，而是接收 Surface
 * - SurfaceTexture 由 Renderer 在 GL 线程中创建
 * - 数据传递方向：Renderer → CameraHelper
 */
package com.example.glearning

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Size
import android.view.Surface
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "CameraHelper"
        private const val DEFAULT_PREVIEW_WIDTH = 640
        private const val DEFAULT_PREVIEW_HEIGHT = 480
    }
    
    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var isCameraOpened: Boolean = false
    
    /**
     * 设置预览 Surface
     *
     * 由 Renderer 调用，将 SurfaceTexture 的 Surface 传入
     * 相机预览将输出到这个 Surface
     *
     * @param surface 从 SurfaceTexture 获取的 Surface
     */
    fun setPreviewSurface(surface: Surface) {
        this.previewSurface = surface
    }
    
    /**
     * 启动相机预览
     *
     * 流程：
     * 1. 检查权限
     * 2. 打开相机设备
     * 3. 创建预览请求
     * 4. 启动 CaptureSession
     */
    fun startCamera() {
        // TODO: 实现相机启动
    }
    
    /**
     * 停止相机预览
     *
     * 流程：
     * 1. 关闭 CaptureSession
     * 2. 关闭 CameraDevice
     * 3. 清理资源
     */
    fun stopCamera() {
        // TODO: 实现相机停止
    }
    
    /**
     * 检查相机权限
     */
    fun hasCameraPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}
```

- [ ] **Step 2: 提交骨架**

```bash
git add app/src/main/java/com/example/glearning/CameraHelper.kt
git commit -m "feat(day15): 创建 CameraHelper 类骨架"
```

### Task 3: 实现 CameraHelper 的相机启动逻辑

**Files:**
- Modify: `app/src/main/java/com/example/glearning/CameraHelper.kt`

- [ ] **Step 1: 实现选择后置摄像头的方法**

在 CameraHelper 类中添加：

```kotlin
    /**
     * 获取后置摄像头 ID
     *
     * 遍历所有摄像头，找到第一个后置摄像头
     * 简化实现：不考虑多摄像头场景
     */
    private fun getBackCameraId(): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取摄像头列表失败: ${e.message}")
        }
        return null
    }
```

- [ ] **Step 2: 实现打开相机设备的方法**

在 CameraHelper 类中添加：

```kotlin
    /**
     * 打开相机设备
     *
     * 使用 CameraManager.openCamera() 打开相机
     * CameraDevice.StateCallback 处理打开成功/失败事件
     */
    private fun openCamera(cameraId: String) {
        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "相机设备已打开")
                    cameraDevice = camera
                    isCameraOpened = true
                    startPreview()
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "相机设备断开连接")
                    camera.close()
                    cameraDevice = null
                    isCameraOpened = false
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "相机设备打开失败: error=$error")
                    camera.close()
                    cameraDevice = null
                    isCameraOpened = false
                }
            }, cameraExecutor)
        } catch (e: Exception) {
            Log.e(TAG, "打开相机失败: ${e.message}")
        }
    }
```

- [ ] **Step 3: 实现 startCamera() 方法**

修改 startCamera() 方法：

```kotlin
    fun startCamera() {
        if (!hasCameraPermission()) {
            Log.w(TAG, "没有相机权限，无法启动相机")
            return
        }
        
        if (previewSurface == null) {
            Log.w(TAG, "预览 Surface 未设置，无法启动相机")
            return
        }
        
        val cameraId = getBackCameraId()
        if (cameraId == null) {
            Log.e(TAG, "找不到后置摄像头")
            return
        }
        
        openCamera(cameraId)
    }
```

- [ ] **Step 4: 提交相机启动逻辑**

```bash
git add app/src/main/java/com/example/glearning/CameraHelper.kt
git commit -m "feat(day15): 实现 CameraHelper 相机启动逻辑"
```

### Task 4: 实现 CameraHelper 的预览请求和停止逻辑

**Files:**
- Modify: `app/src/main/java/com/example/glearning/CameraHelper.kt`

- [ ] **Step 1: 实现创建预览请求的方法**

在 CameraHelper 类中添加：

```kotlin
    /**
     * 创建预览 CaptureRequest
     *
     * 使用 TEMPLATE_PREVIEW 模板创建基础请求
     * 将 Surface 添加为预览目标
     */
    private fun createPreviewRequest(): CaptureRequest? {
        val device = cameraDevice ?: return null
        val surface = previewSurface ?: return null
        
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)
            return builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "创建预览请求失败: ${e.message}")
            return null
        }
    }
```

- [ ] **Step 2: 实现启动预览会话的方法**

在 CameraHelper 类中添加：

```kotlin
    /**
     * 启动预览
     *
     * 流程：
     * 1. 创建 CaptureRequest
     * 2. 创建 CaptureSession
     * 3. 设置连续预览请求
     */
    private fun startPreview() {
        val device = cameraDevice ?: return
        val surface = previewSurface ?: return
        val request = createPreviewRequest() ?: return
        
        try {
            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "CaptureSession 已配置")
                        captureSession = session
                        try {
                            session.setRepeatingRequest(request, null, cameraExecutor)
                            Log.d(TAG, "预览已启动")
                        } catch (e: Exception) {
                            Log.e(TAG, "设置预览请求失败: ${e.message}")
                        }
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "CaptureSession 配置失败")
                    }
                },
                cameraExecutor
            )
        } catch (e: Exception) {
            Log.e(TAG, "创建 CaptureSession 失败: ${e.message}")
        }
    }
```

- [ ] **Step 3: 实现停止相机的方法**

修改 stopCamera() 方法：

```kotlin
    fun stopCamera() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            isCameraOpened = false
            Log.d(TAG, "相机已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止相机失败: ${e.message}")
        }
    }
```

- [ ] **Step 4: 提交预览和停止逻辑**

```bash
git add app/src/main/java/com/example/glearning/CameraHelper.kt
git commit -m "feat(day15): 实现 CameraHelper 预览请求和停止逻辑"
```

---

## Chunk 3: Day15Renderer 类

### Task 5: 创建 Day15Renderer 类骨架

**Files:**
- Create: `app/src/main/java/com/example/glearning/Day15Renderer.kt`

- [ ] **Step 1: 创建 Day15Renderer 类基础结构**

```kotlin
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

class Day15Renderer(private val cameraHelper: CameraHelper) : GLSurfaceView.Renderer {
    
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
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // TODO: 实现初始化
    }
    
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // TODO: 实现视口设置
    }
    
    override fun onDrawFrame(gl: GL10?) {
        // TODO: 实现渲染
    }
}
```

- [ ] **Step 2: 提交骨架**

```bash
git add app/src/main/java/com/example/glearning/Day15Renderer.kt
git commit -m "feat(day15): 创建 Day15Renderer 类骨架"
```

### Task 6: 实现 Day15Renderer 的 OES 纹理创建

**Files:**
- Modify: `app/src/main/java/com/example/glearning/Day15Renderer.kt`

- [ ] **Step 1: 在 companion object 中添加辅助方法**

在 companion object 中添加：

```kotlin
        /**
         * 加载着色器
         *
         * @param type 色器类型（GL_VERTEX_SHADER 或 GL_FRAGMENT_SHADER）
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
```

- [ ] **Step 2: 实现创建 OES 纹理的方法**

在 Day15Renderer 类中添加：

```kotlin
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
```

- [ ] **Step 3: 实现创建 SurfaceTexture 的方法**

在 Day15Renderer 类中添加：

```kotlin
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
        }
        
        // 将 Surface 传给 CameraHelper
        // CameraHelper 将其设置为相机预览目标
        cameraHelper.setPreviewSurface(surfaceTexture!!.surface)
        
        surfaceTextureReady = true
        Log.d(TAG, "SurfaceTexture 已创建")
    }
```

- [ ] **Step 4: 提交纹理创建逻辑**

```bash
git add app/src/main/java/com/example/glearning/Day15Renderer.kt
git commit -m "feat(day15): 实现 Day15Renderer OES 纹理和 SurfaceTexture 创建"
```

### Task 7: 实现 Day15Renderer 的着色器程序和渲染循环

**Files:**
- Modify: `app/src/main/java/com/example/glearning/Day15Renderer.kt`

- [ ] **Step 1: 实现 onSurfaceCreated() 方法**

修改 onSurfaceCreated() 方法：

```kotlin
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 设置背景色（深灰色）
        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1.0f)
        
        // 编译着色器
        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
        
        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "着色器编译失败")
            return
        }
        
        // 创建着色器程序
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vertexShader)
        GLES20.glAttachShader(program, fragmentShader)
        GLES20.glLinkProgram(program)
        
        // 检查链接状态
        val linked = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == GLES20.GL_FALSE) {
            Log.e(TAG, "着色器程序链接失败: ${GLES20.glGetProgramInfoLog(program)}")
            return
        }
        
        // 获取 attribute 和 uniform 的位置
        positionHandle = GLES20.glGetAttribLocation(program, "a_Position")
        textureCoordHandle = GLES20.glGetAttribLocation(program, "a_TextureCoord")
        matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
        textureTransformHandle = GLES20.glGetUniformLocation(program, "u_TextureTransform")
        textureHandle = GLES20.glGetUniformLocation(program, "u_Texture")
        
        // 创建顶点缓冲区
        val vb = ByteBuffer.allocateDirect(FULL_SCREEN_RECT.size * FLOAT_SIZE)
        vb.order(ByteOrder.nativeOrder())
        vertexBuffer = vb.asFloatBuffer()
        vertexBuffer?.put(FULL_SCREEN_RECT)
        vertexBuffer?.position(0)
        
        // 创建 SurfaceTexture（必须在 GL 线程）
        createSurfaceTexture()
        
        // 初始化变换矩阵
        Matrix.setIdentityM(textureTransformMatrix, 0)
        Matrix.setIdentityM(mvpMatrix, 0)
        
        Log.d(TAG, "Renderer 初始化完成")
    }
```

- [ ] **Step 2: 实现 onSurfaceChanged() 方法**

修改 onSurfaceChanged() 方法：

```kotlin
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        // 设置视口
        GLES20.glViewport(0, 0, width, height)
        
        // 相机预览通常是全屏，使用单位矩阵即可
        // 如果需要调整显示比例，可以在这里设置正交投影矩阵
        Matrix.setIdentityM(mvpMatrix, 0)
        
        Log.d(TAG, "视口设置完成: width=$width, height=$height")
    }
```

- [ ] **Step 3: 实现 onDrawFrame() 方法**

修改 onDrawFrame() 方法：

```kotlin
    override fun onDrawFrame(gl: GL10?) {
        // 清除缓冲区
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        
        // 检查 SurfaceTexture 是否就绪
        if (!surfaceTextureReady || surfaceTexture == null) {
            // SurfaceTexture 未就绪，显示背景色
            return
        }
        
        // 更新纹理内容
        // 从相机获取最新帧并写入 OES 纹理
        // 必须在 GL 线程中调用
        surfaceTexture?.updateTexImage()
        
        // 获取变换矩阵
        // 此矩阵用于校正相机传感器的旋转和裁剪
        // 如果不使用此矩阵，画面可能显示不正确
        surfaceTexture?.getTransformMatrix(textureTransformMatrix)
        
        // 使用着色器程序
        GLES20.glUseProgram(program)
        
        // 设置 MVP 矩阵（全屏显示，使用单位矩阵）
        GLES20.glUniformMatrix4fv(matrixHandle, 1, false, mvpMatrix, 0)
        
        // 设置纹理变换矩阵
        GLES20.glUniformMatrix4fv(textureTransformHandle, 1, false, textureTransformMatrix, 0)
        
        // 绑定 OES 纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, oesTextureId)
        GLES20.glUniform1i(textureHandle, 0)
        
        // 绘制全屏矩形
        drawFullScreenRect()
        
        // 解绑纹理
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
    }
```

- [ ] **Step 4: 实现绘制全屏矩形的方法**

在 Day15Renderer 类中添加：

```kotlin
    /**
     * 绘制全屏矩形
     *
     * 使用 GL_TRIANGLE_STRIP 绘制矩形
     * 4 个顶点，绘制顺序：0-1-2-3
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
    }
```

- [ ] **Step 5: 提交渲染逻辑**

```bash
git add app/src/main/java/com/example/glearning/Day15Renderer.kt
git commit -m "feat(day15): 实现 Day15Renderer 着色器程序和渲染循环"
```

---

## Chunk 4: MainActivity 集成

### Task 8: 修改 MainActivity 添加权限处理

**Files:**
- Modify: `app/src/main/java/com/example/glearning/MainActivity.kt`

- [ ] **Step 1: 修改 MainActivity 添加权限请求**

```kotlin
package com.example.glearning

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * MainActivity: 权限请求 + 生命周期管理
 *
 * 关键流程：
 * 1. onCreate: 检查权限 + 创建 GLSurfaceView + 创建 CameraHelper + 创建 Renderer
 * 2. onResume: glSurfaceView.onResume() + cameraHelper.startCamera()
 * 3. onPause: cameraHelper.stopCamera() + glSurfaceView.onPause()
 *
 * 数据传递方向：
 * - Renderer.onSurfaceCreated() 创建 SurfaceTexture → 将其 Surface 传给 CameraHelper
 * - CameraHelper.setPreviewSurface(surface) 接收 Surface → 用于相机预览
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var cameraHelper: CameraHelper
    
    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 创建 GLSurfaceView
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        
        // 创建 CameraHelper
        cameraHelper = CameraHelper(this)
        
        // 创建 Renderer（传入 CameraHelper）
        // Renderer 在 onSurfaceCreated 中会创建 SurfaceTexture 并传给 CameraHelper
        val renderer = Day15Renderer(cameraHelper)
        glSurfaceView.setRenderer(renderer)
        
        setContentView(glSurfaceView)
        
        // 检查相机权限
        checkCameraPermission()
    }
    
    /**
     * 检查相机权限
     *
     * 如果有权限，相机将在 onResume 时启动
     * 如果无权限，请求权限
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // 已有权限
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    /**
     * 权限请求结果回调
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "相机权限已授予", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "相机权限被拒绝，无法显示预览", Toast.LENGTH_LONG).show()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        
        // 启动相机
        // 注意：此时 Renderer.onSurfaceCreated() 已执行
        // SurfaceTexture 已创建并传给 CameraHelper
        if (cameraHelper.hasCameraPermission()) {
            cameraHelper.startCamera()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 停止相机（先停止相机，再暂停 GLSurfaceView）
        cameraHelper.stopCamera()
        glSurfaceView.onPause()
    }
}
```

- [ ] **Step 2: 提交 MainActivity 修改**

```bash
git add app/src/main/java/com/example/glearning/MainActivity.kt
git commit -m "feat(day15): MainActivity 添加相机权限和生命周期管理"
```

---

## Chunk 5: 验证与文档

### Task 9: 编写 Day 15 学习笔记

**Files:**
- Create: `openspec/specs/learning-plan/day15-notes.md`

- [ ] **Step 1: 创建 Day 15 学习笔记**

```markdown
# Day 15: SurfaceTexture + OES 外部纹理

## 学习目标

- 理解 GL_TEXTURE_EXTERNAL_OES 外部纹理
- 掌握 OES_EGL_image_external 着色器扩展
- 实现 SurfaceTexture 连接相机和 OpenGL
- 使用纹理变换矩阵处理传感器方向

---

## 核心概念

### 1. OES 外部纹理

**什么是 OES 纹理？**
- OpenGL ES 扩展：GL_OES_EGL_image_external
- 用于显示相机、视频等外部数据源
- 与 GL_TEXTURE_2D 的区别：
  - 不能设置纹理内容（glTexImage2D 无效）
  - 内容由外部数据源（SurfaceTexture）提供
  - 只能使用 GL_LINEAR 或 GL_NEAREST

**着色器扩展：**
```glsl
#extension GL_OES_EGL_image_external : require
uniform samplerExternalOES u_Texture;  // 专用采样器
```

### 2. SurfaceTexture

**作用：**
- 连接相机输出和 OpenGL 纹理
- 相机帧写入 SurfaceTexture → 更新 OES 纹理 → OpenGL 渲染

**关键方法：**
- `SurfaceTexture(textureId)`：从 OES 纹理 ID 创建
- `setDefaultBufferSize()`：设置预览尺寸
- `updateTexImage()`：更新纹理内容（必须在 GL 线程）
- `getTransformMatrix()`：获取 UV 变换矩阵

**线程约束：**
- 创建必须在 GL 线程
- updateTexImage() 必须在 GL 线程
- requestRender() 可以在任意线程

### 3. 纹理变换矩阵

**为什么需要？**
- 相机传感器有固定方向（通常是横向）
- 手机可能竖屏使用
- 不同设备传感器方向不同

**u_TextureTransform 的作用：**
- 校正 UV 坐标
- 处理传感器旋转和裁剪
- 确保画面正确显示

---

## 架构设计

```
MainActivity
  │
  ├─ CameraHelper (相机管理)
  │   └─ 接收 Surface → 设置预览目标
  │
  └─ Day15Renderer (OES 纹理渲染)
      └─ 创建 SurfaceTexture → 传给 CameraHelper
```

**数据流：**
1. Renderer.onSurfaceCreated() → 创建 OES 纹理 → 创建 SurfaceTexture → 传给 CameraHelper
2. CameraHelper.startCamera() → 相机输出到 SurfaceTexture
3. 帧到达 → requestRender() → onDrawFrame() → updateTexImage() → 渲染

---

## 关键代码解析

### 创建 SurfaceTexture（必须在 GL 线程）

```kotlin
// 创建 OES 纹理
val textureId = createOESTexture()

// 从纹理 ID 创建 SurfaceTexture
surfaceTexture = SurfaceTexture(textureId)

// 设置预览尺寸
surfaceTexture?.setDefaultBufferSize(640, 480)

// 设置帧可用监听器
surfaceTexture?.setOnFrameAvailableListener {
    requestRender()  // 触发 onDrawFrame
}

// 将 Surface 传给 CameraHelper
cameraHelper.setPreviewSurface(surfaceTexture!!.surface)
```

### 更新纹理并渲染（GL 线程）

```kotlin
// 更新纹理内容（从相机获取最新帧）
surfaceTexture?.updateTexImage()

// 获取变换矩阵（校正 UV 坐标）
surfaceTexture?.getTransformMatrix(textureTransformMatrix)

// 设置矩阵
GLES20.glUniformMatrix4fv(textureTransformHandle, 1, false, textureTransformMatrix, 0)

// 绘制
GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
```

---

## 常见问题

### Q1: 画面显示不正确（旋转、拉伸）

**原因：** 未使用 u_TextureTransform 矩阵

**解决：** 在 onDrawFrame 中调用 `getTransformMatrix()` 并传给着色器

### Q2: 编译错误 "samplerExternalOES not defined"

**原因：** 未添加 OES 扩展

**解决：** 在着色器开头添加 `#extension GL_OES_EGL_image_external : require`

### Q3: 画面黑屏

**可能原因：**
1. 相机权限未授予
2. SurfaceTexture 未创建
3. updateTexImage() 未调用

---

## 下一步

**Day 16**: 基础滤镜 - 亮度/对比度/饱和度
- 在 OES 纹理上应用片段着色器滤镜
- 实现可调节的图像参数
```

- [ ] **Step 2: 提交学习笔记**

```bash
git add openspec/specs/learning-plan/day15-notes.md
git commit -m "docs(day15): Day 15 学习笔记"
```

### Task 10: 更新学习计划进度

**Files:**
- Modify: `openspec/specs/learning-plan/spec.md`

- [ ] **Step 1: 更新进度跟踪表**

在 spec.md 的进度跟踪部分添加 Day 15：

```markdown
| 第 3 周 | 相机纹理与基础滤镜 | 🔄 Day 15 进行中 |
```

在实际学习进度表中添加：

```markdown
| Day 15 | 相机预览到 GLSurfaceView | SurfaceTexture + OES 纹理 | ✅ |
```

并更新下一步：

```markdown
**下一步**：进入 Day 16 - 基础滤镜（亮度/对比度/饱和度）
```

- [ ] **Step 2: 提交进度更新**

```bash
git add openspec/specs/learning-plan/spec.md
git commit -m "docs(day15): 更新学习进度 - Day 15 完成"
```

---

## 最终验证

- [ ] **Step 1: 构建项目**

```bash
./gradlew assembleDebug
```

预期输出：`BUILD SUCCESSFUL`

- [ ] **Step 2: 提交所有更改**

```bash
git status
git add -A
git commit -m "feat(day15): 完成 Day 15 - SurfaceTexture + OES 外部纹理"
```

---

## 学习目标验证问题

完成后应能回答：
1. 什么是 OES 外部纹理？与 GL_TEXTURE_2D 有何区别？
2. SurfaceTexture 如何连接 Camera2 和 OpenGL？
3. 为什么需要 u_TextureTransform 矩阵？
4. updateTexImage() 在哪个线程调用？为什么？
5. requestRender() 的作用是什么？