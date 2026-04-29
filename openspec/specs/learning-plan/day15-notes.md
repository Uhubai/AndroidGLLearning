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
    glSurfaceView.requestRender()  // 触发 onDrawFrame
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

## 文件结构

| 文件 | 职责 |
|------|------|
| CameraHelper.kt | Camera2 预览封装 |
| Day15Renderer.kt | OES 纹理渲染 |
| MainActivity.kt | 权限 + 生命周期管理 |

---

## 下一步

**Day 16**: 基础滤镜 - 亮度/对比度/饱和度
- 在 OES 纹理上应用片段着色器滤镜
- 实现可调节的图像参数