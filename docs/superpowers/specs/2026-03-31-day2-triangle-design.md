# Day 2: 渐变色三角形设计

## 目标

绘制渐变色三角形，理解着色器和归一化设备坐标 (NDC)

## 架构

```
app/src/main/java/com/example/glearning/
├── MainActivity.kt           # 修改：切换到 Day2Renderer
├── Day1Renderer.kt          # Day 1 渲染器（保留）
└── Day2Renderer.kt          # Day 2 渲染器（新建）
```

## 实现要点

### 1. 顶点数据

- 三个顶点，每个顶点 5 个 float：x, y, r, g, b
- 使用 `FloatBuffer` 存储顶点数据
- NDC 坐标范围：[-1, 1]

### 2. 着色器

**顶点着色器**：
- 接收位置属性 `a_Position`
- 接收颜色属性 `a_Color`
- 传递颜色到片段着色器

**片段着色器**：
- 接收插值颜色
- 输出最终颜色

### 3. 绘制流程

1. `onSurfaceCreated`: 编译着色器，创建程序，初始化顶点缓冲
2. `onDrawFrame`: 绑定属性，绘制三角形

## 技术细节

- OpenGL ES 2.0
- 使用 `GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)`
- 着色器代码内嵌为字符串常量