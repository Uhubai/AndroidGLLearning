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
          │ SurfaceTexture               │ setRenderer
          ▼                              ▼
┌──────────────────────┐       ┌──────────────────────┐
│    CameraHelper      │       │    Day15Renderer     │
├──────────────────────┤       ├──────────────────────┤
│ CameraManager        │       │ OES 纹理 ID          │
│ CameraDevice         │       │ SurfaceTexture 引用  │
│ CaptureSession       │──────▶│ OES 着色器程序       │
│ SurfaceTexture       │       │ MVP 矩阵             │
│                      │       │ 渲染循环              │
└──────────────────────┘       └──────────────────────┘
```

**关键数据流**：
1. CameraHelper 创建 SurfaceTexture 并传给 Day15Renderer
2. 相机帧到达 → SurfaceTexture 通知 → Day15Renderer 更新纹理 → requestRender()
3. GL 线程执行 `onDrawFrame()`：`updateTexImage()` → 渲染 OES 纹理到屏幕

---

## CameraHelper 类

### 职责

封装 Camera2 预览配置，提供 SurfaceTexture 给渲染器。

### 核心方法

| 方法 | 功能 |
|------|------|
| `startCamera(width, height)` | 打开相机，配置预览尺寸，启动预览 |
| `stopCamera()` | 关闭相机设备，释放资源 |
| `getSurfaceTexture()` | 返回 SurfaceTexture 给 Day15Renderer |
| `setOnFrameAvailableListener()` | 设置帧可用回调（由 Renderer 调用） |

### Camera2 配置简化

- 使用第一个后置摄像头
- 预览尺寸选择：优先匹配传入尺寸，否则选择最接近的
- 不处理闪光灯、HDR 等高级功能

### 关键流程

```
startCamera()
  → cameraManager.openCamera()
  → cameraDevice.createCaptureRequest(TEMPLATE_PREVIEW)
  → surfaceTexture.setDefaultBufferSize()
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

```
onSurfaceCreated()
  → glGenTextures() + glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
  → 设置纹理参数（GL_LINEAR 等）
  
onDrawFrame()
  → surfaceTexture.updateTexImage()  // 从相机获取最新帧
  → surfaceTexture.getTransformMatrix()  // 获取变换矩阵
  → glUniformMatrix4fv(u_TextureTransform)
  → glDrawElements()
```

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
  → renderer.setCameraHelper(cameraHelper)
  
onResume()
  → glSurfaceView.onResume()
  → cameraHelper.startCamera()

onPause()
  → cameraHelper.stopCamera()
  → glSurfaceView.onPause()
```

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