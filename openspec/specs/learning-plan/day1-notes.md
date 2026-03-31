# Day 1: 项目搭建 + 渲染管线

## 学习目标

- 搭建 Android OpenGL ES 项目
- 理解 GPU 渲染管线
- 绘制纯色背景

## 代码结构

```
app/src/main/java/com/example/glearning/
├── MainActivity.kt      # 主 Activity，管理 GLSurfaceView
└── (Day1Renderer)       # Day 1 渲染器（内嵌在 MainActivity 中）
```

---

## 核心概念

### 1. GLSurfaceView

Android 提供的 OpenGL ES 渲染表面，封装了 EGL 上下文管理：

```kotlin
glSurfaceView = GLSurfaceView(this)
glSurfaceView.setEGLContextClientVersion(2)  // 使用 OpenGL ES 2.0
glSurfaceView.setRenderer(Day1Renderer())
```

### 2. Renderer 接口

渲染器需要实现三个回调：

| 方法 | 触发时机 | 用途 |
|------|----------|------|
| `onSurfaceCreated` | Surface 创建时 | 初始化资源、设置背景色 |
| `onSurfaceChanged` | 尺寸变化时 | 设置视口 |
| `onDrawFrame` | 每帧 | 执行绑定命令 |

### 3. 渲染管线概览

```
┌─────────────────────────────────────────────────────────────────────┐
│                      OpenGL ES 渲染管线                              │
└─────────────────────────────────────────────────────────────────────┘

  顶点数据                着色器程序
     │                      │
     ▼                      ▼
┌─────────┐          ┌─────────────┐
│  顶点   │─────────▶│  顶点着色器  │  ← 必须编写
│  缓冲区 │          │ (Vertex     │     处理每个顶点
└─────────┘          │  Shader)    │
                     └──────┬──────┘
                            │
                            ▼
                     ┌─────────────┐
                     │  图元装配   │  组装点/线/三角形
                     └──────┬──────┘
                            │
                            ▼
                     ┌─────────────┐
                     │   光栅化     │  顶点 → 片段（像素）
                     └──────┬──────┘
                            │
                            ▼
                     ┌─────────────┐
                     │  片段着色器  │  ← 必须编写
                     │ (Fragment  │     计算每个像素颜色
                     │  Shader)   │
                     └──────┬──────┘
                            │
                            ▼
                     ┌─────────────┐
                     │  帧缓冲区   │  最终显示到屏幕
                     └─────────────┘
```

### 4. 今天的关键代码

```kotlin
override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    // 设置清屏颜色 (R, G, B, A)
    // 范围: 0.0 ~ 1.0
    GLES20.glClearColor(0.1f, 0.2f, 0.4f, 1.0f)
}

override fun onDrawFrame(gl: GL10?) {
    // 用设置的颜色清除颜色缓冲区
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
}
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **GLSurfaceView** | Android 提供的 OpenGL 渲染表面 |
| **Renderer** | 渲染器接口，定义绑制回调 |
| **EGL** | OpenGL 与原生窗口系统的接口 |
| **视口 (Viewport)** | OpenGL 渲染输出的矩形区域 |
| **帧缓冲区** | 存储最终渲染结果的内存 |
| **颜色缓冲区** | 帧缓冲区中存储颜色的部分 |

---

## 练习

1. **修改背景色**：尝试不同的 RGBA 值，观察效果
   - 红色：`(1.0f, 0.0f, 0.0f, 1.0f)`
   - 绿色：`(0.0f, 1.0f, 0.0f, 1.0f)`
   - 半透明：`(0.5f, 0.5f, 0.5f, 0.5f)`

2. **思考**：为什么 `glClearColor` 和 `glClear` 分开调用？

---

## 常见问题

**Q: 为什么选择 OpenGL ES 2.0 而不是 3.0？**
A: 2.0 覆盖更广泛的设备，核心概念相同。学完 2.0 后可轻松过渡到 3.0。

**Q: `glClear` 清除的是什么？**
A: 颜色缓冲区。还可以清除深度缓冲区 (`GL_DEPTH_BUFFER_BIT`) 和模板缓冲区 (`GL_STENCIL_BUFFER_BIT`)。

**Q: 为什么需要 `setEGLContextClientVersion(2)`？**
A: 默认使用 OpenGL ES 1.1（固定管线），我们需要 2.0（可编程管线）。

---

## 下一步

Day 2 将学习：
- 编写顶点着色器
- 编写片段着色器
- 绘制第一个三角形