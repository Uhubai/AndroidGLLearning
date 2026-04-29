# Day 15 设计文档：SurfaceTexture + OES 外部纹理

## 目标

实现相机预览到 GLSurfaceView，核心学习：
- SurfaceTexture 作为相机输出 Surface
- GL_TEXTURE_EXTERNAL_OES 外部纹理
- OES_EGL_image_external 着色器扩展
- 纹理变换矩阵处理传感器方向

## 整体架构

```
┌─────────────────────────────────────────────────────────────┐
│                        MainActivity                          │
│  - 权限请求                                                  │
│  - 创建 GLSurfaceView                                       │
│  - 创建 CameraHelper                                        │
│  - 协调生命周期                                              │
└─────────────────────────────────────────────────────────────┘
          │                              │
          │ Surface (from SurfaceTexture)│ setRenderer
          ▼                              ▼
┌──────────────────────┐       ┌──────────────────────┐
│    CameraHelper      │       │    Day15Renderer     │
├──────────────────────┤       ├──────────────────────┤
│ CameraManager        │       │ OES 纹理 ID          │
│ CameraDevice         │◀──────│ SurfaceTexture       │
│ CaptureSession       │       │ OES 着色器程序       │
│ Preview Surface      │       │ MVP 矩阵             │
│                      │       │ 渲染循环              │
└──────────────────────┘       └──────────────────────┘
```

**关键数据流**：
1. Day15Renderer 在 GL 线程中创建 OES 纹理 → 创建 SurfaceTexture → 将其 Surface 传给 CameraHelper
2. CameraHelper 将 Surface 设为相机预览目标 → 相机帧写入 SurfaceTexture
3. 相机帧到达 → SurfaceTexture 通知 → Day15Renderer.requestRender()
4. GL 线程执行 `onDrawFrame()`：`updateTexImage()` → 渲染 OES 纹理到屏幕

**重要约束**：
- SurfaceTexture 必须在 GL 线程中从 OES 纹理 ID 创建
- CameraHelper 不能在非 GL 线程创建 SurfaceTexture
- 数据传递方向：Renderer → CameraHelper（不是反向）

---

## CameraHelper 类

### 职责

封装 Camera2 预览配置，接收来自 Renderer 的 Surface 作为相机预览目标。

**注意**：CameraHelper 不创建 SurfaceTexture，而是接收从 SurfaceTexture 获取的 Surface。

### 核心方法

| 方法 | 功能 |
|------|------|
| `setPreviewSurface(surface)` | 接收相机预览 Surface（从 SurfaceTexture 获取） |
| `startCamera()` | 打开相机，启动预览（使用已设置的 Surface） |
| `stopCamera()` | 关闭相机设备，释放资源 |
| `onCameraOpened()` | 回调通知相机已打开（可选） |

### Camera2 配置简化

- 使用第一个后置摄像头
- 预览尺寸：由 Renderer 设置 SurfaceTexture 的默认缓冲区大小
- 不处理闪光灯、HDR 等高级功能

### 关键流程

```
setPreviewSurface(surface)
  → 保存 Surface 引用

startCamera()
  → cameraManager.openCamera()
  → cameraDevice.createCaptureRequest(TEMPLATE_PREVIEW)
  → captureRequest.addTarget(savedSurface)
  → captureSession.setRepeatingRequest()
```

---

## Day15Renderer - OES 着色器与纹理

### OES 外部纹理的关键差异

| 特性 | GL_TEXTURE_2D（Day10-14） | GL_TEXTURE_EXTERNAL_OES（Day15） |
|------|---------------------------|----------------------------------|
| 着色器扩展 | 不需要 | `#extension GL_OES_EGL_image_external : require` |
| 采样器类型 | `sampler2D` | `samplerExternalOES` |
| 纹理格式 | 标准 RGBA | 相机原始格式（自动转换） |
| UV 坐标 | 标准 [0, 1] | 可能需要矩阵变换（相机传感器方向） |

### 顶点着色器

```glsl
#extension GL_OES_EGL_image_external : require
uniform mat4 u_Matrix;
uniform mat4 u_TextureTransform;  // SurfaceTexture.getTransformMatrix()
attribute vec4 a_Position;
attribute vec2 a_TextureCoord;
varying vec2 v_TextureCoord;

void main() {
    gl_Position = u_Matrix * a_Position;
    v_TextureCoord = (u_TextureTransform * vec4(a_TextureCoord, 0.0, 1.0)).xy;
}
```

**关键点**：
- `u_TextureTransform` 来自 SurfaceTexture.getTransformMatrix()
- 用于处理相机传感器的旋转和裁剪
- 不同设备有不同的传感器方向，必须使用此矩阵

### 片段着色器

```glsl
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 v_TextureCoord;
uniform samplerExternalOES u_Texture;

void main() {
    gl_FragColor = texture2D(u_Texture, v_TextureCoord);
}
```

### 纹理创建与更新流程

**onSurfaceCreated()（GL 线程）**：
```
1. 创建 OES 纹理 ID
   → glGenTextures() 
   → glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
   → 设置纹理参数（GL_LINEAR 等）

2. 从纹理 ID 创建 SurfaceTexture
   → surfaceTexture = SurfaceTexture(textureId)
   → surfaceTexture.setDefaultBufferSize(640, 480)  // 预览尺寸

3. 设置帧可用监听器
   → surfaceTexture.setOnFrameAvailableListener { requestRender() }

4. 将 Surface 传给 CameraHelper
   → cameraHelper.setPreviewSurface(surfaceTexture.surface)
```

**onDrawFrame()（GL 线程）**：
```
1. 更新纹理内容
   → surfaceTexture.updateTexImage()  // 从相机获取最新帧

2. 获取变换矩阵
   → surfaceTexture.getTransformMatrix(textureTransformMatrix)

3. 渲染到屏幕
   → glUniformMatrix4fv(u_TextureTransform)
   → glDrawElements()
```

**关键约束**：
- `SurfaceTexture(textureId)` 必须在 GL 线程中调用
- `updateTexImage()` 必须在 GL 线程中调用
- `getTransformMatrix()` 获取的是 4x4 矩阵，用于校正 UV 坐标

---

## 线程同步与 MainActivity 集成

### 线程模型

```
相机线程               GL 渲染线程
──────────             ──────────
帧到达 ────▶ OnFrameAvailableListener
                     │
                     │ requestRender()
                     ▼
                 onDrawFrame()
                     │
                     │ updateTexImage()
                     ▼
                 渲染到屏幕
```

### 关键点

- `SurfaceTexture.setOnFrameAvailableListener()` 在 GL 线程中设置
- `requestRender()` 通知 GLSurfaceView 执行渲染
- `updateTexImage()` 必须在 GL 线程中调用

### MainActivity 生命周期管理

```kotlin
onCreate()
  → 检查相机权限
  → 创建 GLSurfaceView
  → 创建 CameraHelper
  → 创建 Day15Renderer(cameraHelper)  // 将 CameraHelper 传给 Renderer
  → glSurfaceView.setRenderer(renderer)
  
onResume()
  → glSurfaceView.onResume()
  → cameraHelper.startCamera()  // Renderer 已将 Surface 传给 CameraHelper

onPause()
  → cameraHelper.stopCamera()
  → glSurfaceView.onPause()
```

**数据传递方向**：
- Renderer.onSurfaceCreated() 创建 SurfaceTexture → 将其 Surface 传给 CameraHelper
- CameraHelper.setPreviewSurface(surface) 接收 Surface → 用于相机预览

### 权限处理

- 在 AndroidManifest 中声明 `CAMERA` 权限
- MainActivity.onCreate 中检查权限，有则启动，无则请求
- 使用 Activity 结果 API（registerForActivityResult）

---

## 错误处理与边界情况

### 相机错误处理

| 错误类型 | 处理方式 |
|----------|----------|
| 相机权限拒绝 | Toast 提示 + 不启动相机 |
| 相机设备打开失败 | Log.e + 空预览（不影响 OpenGL） |
| 预览尺寸不匹配 | 自动选择最接近尺寸 |

### OpenGL 错误处理

| 错误类型 | 处理方式 |
|----------|----------|
| OES 纹理创建失败 | Log.e + 不渲染 |
| 着色器编译失败 | Log.e + 使用默认颜色 |
| SurfaceTexture 未初始化 | 不调用 updateTexImage() |

### 边界情况

- **相机不可用**：Renderer 显示纯色背景或提示文字
- **SurfaceTexture 尚未创建**：onDrawFrame 中判断 `surfaceTexture != null`
- **预览尺寸为 0**：CameraHelper 使用默认 640x480

---

## 代码注释重点

根据 AGENTS.md 要求，以下内容必须详细注释：

1. **OES 扩展的必要性**：解释为何需要 `#extension GL_OES_EGL_image_external`
2. **SurfaceTexture.getTransformMatrix()**：说明其作用和必要性
3. **线程同步原理**：解释为何 requestRender 在非 GL 线程调用
4. **Camera2 预览请求的生命周期**：openCamera → createCaptureRequest → setRepeatingRequest
5. **updateTexImage() 时序**：为何必须在 onDrawFrame 中调用

---

## 文件清单

| 文件 | 功能 |
|------|------|
| `CameraHelper.kt` | Camera2 预览封装 |
| `Day15Renderer.kt` | OES 纹理渲染 |
| `MainActivity.kt` | 权限 + 生命周期协调（修改） |
| `AndroidManifest.xml` | 添加 CAMERA 权限 |

---

## 学习目标验证

完成后应能回答：
1. 什么是 OES 外部纹理？与 GL_TEXTURE_2D 有何区别？
2. SurfaceTexture 如何连接 Camera2 和 OpenGL？
3. 为什么需要 u_TextureTransform 矩阵？
4. updateTexImage() 在哪个线程调用？为什么？
5. requestRender() 的作用是什么？