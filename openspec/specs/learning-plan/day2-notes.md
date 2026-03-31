# Day 2: 顶点着色器 + 第一个三角形

## 学习目标

- 编写顶点着色器和片段着色器
- 理解归一化设备坐标 (NDC)
- 绘制渐变色三角形

## 代码结构

```
app/src/main/java/com/example/glearning/
├── MainActivity.kt       # 主 Activity，切换到 Day2Renderer
├── Day1Renderer.kt       # Day 1 渲染器（保留）
└── Day2Renderer.kt       # Day 2 渲染器（新建）
```

---

## 核心概念

### 1. 归一化设备坐标 (NDC)

OpenGL 使用归一化设备坐标，范围是 [-1, 1]：

```
        y = 1.0
          ↑
          │
          │
          │
x = -1.0 ─┼─────────→ x = 1.0
          │
          │
          │
        y = -1.0
```

- 屏幕中心是 (0, 0)
- 左边界 x = -1，右边界 x = 1
- 上边界 y = 1，下边界 y = -1

### 2. 顶点着色器

顶点着色器处理每个顶点：

```glsl
attribute vec4 a_Position;  // 顶点位置
attribute vec4 a_Color;     // 顶点颜色
varying vec4 v_Color;       // 传递给片段着色器

void main() {
    gl_Position = a_Position;  // 必须设置
    v_Color = a_Color;         // 传递颜色
}
```

**关键字说明**：
| 关键字 | 含义 |
|--------|------|
| `attribute` | 顶点属性，每个顶点不同 |
| `varying` | 插值变量，顶点→片段自动插值 |
| `gl_Position` | 内置变量，顶点最终位置 |

### 3. 片段着色器

片段着色器计算每个像素的颜色：

```glsl
precision mediump float;    // 精度限定符
varying vec4 v_Color;       // 从顶点着色器插值而来

void main() {
    gl_FragColor = v_Color; // 输出颜色
}
```

**精度限定符**：
| 精度 | 说明 |
|------|------|
| `highp` | 高精度 |
| `mediump` | 中精度（推荐用于片段着色器） |
| `lowp` | 低精度 |

### 4. 渲染管线（续 Day 1）

```
顶点数据                    着色器程序
   │                          │
   ▼                          ▼
┌────────────┐         ┌─────────────┐
│ 顶点缓冲区 │────────▶│ 顶点着色器  │ ← 我们在这里
│ (FloatBuffer)│       │ - 处理位置  │
└────────────┘         │ - 传递属性  │
                       └──────┬──────┘
                              │
                              ▼
                       ┌─────────────┐
                       │  图元装配   │ 组装三角形
                       └──────┬──────┘
                              │
                              ▼
                       ┌─────────────┐
                       │   光栅化    │ 生成片段
                       └──────┬──────┘
                              │
                              ▼
                       ┌─────────────┐
                       │ 片段着色器  │ ← 我们在这里
                       │ - 计算颜色  │
                       └──────┬──────┘
                              │
                              ▼
                       ┌─────────────┐
                       │  帧缓冲区   │
                       └─────────────┘
```

---

## 关键代码解析

### 顶点数据布局

```kotlin
// 每个顶点：x, y, r, g, b（共 5 个 float）
private val TRIANGLE_COORDS_AND_COLORS = floatArrayOf(
    // 位置        颜色
     0.0f,  0.5f,  1.0f, 0.0f, 0.0f,  // 顶部 - 红色
    -0.5f, -0.5f,  0.0f, 1.0f, 0.0f,  // 左下 - 绿色
     0.5f, -0.5f,  0.0f, 0.0f, 1.0f   // 右下 - 蓝色
)

// 步长：每个顶点占用的字节数
val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE
// = (2 + 3) * 4 = 20 字节
```

### 着色器编译

```kotlin
private fun loadShader(type: Int, shaderCode: String): Int {
    val shader = GLES20.glCreateShader(type)      // 创建着色器对象
    GLES20.glShaderSource(shader, shaderCode)     // 加载着色器源码
    GLES20.glCompileShader(shader)                 // 编译
    return shader
}
```

### 绑定顶点属性

```kotlin
// 设置位置属性
vertexBuffer?.position(0)  // 从偏移 0 开始
GLES20.glVertexAttribPointer(
    positionHandle,           // 属性位置
    COORDS_PER_VERTEX,        // 每个顶点 2 个分量 (x, y)
    GLES20.GL_FLOAT,          // 数据类型
    false,                    // 不归一化
    stride,                   // 步长
    vertexBuffer              // 数据缓冲
)
GLES20.glEnableVertexAttribArray(positionHandle)

// 设置颜色属性
vertexBuffer?.position(COORDS_PER_VERTEX)  // 从偏移 2 开始
GLES20.glVertexAttribPointer(
    colorHandle,
    COLORS_PER_VERTEX,        // 每个顶点 3 个分量 (r, g, b)
    GLES20.GL_FLOAT,
    false,
    stride,
    vertexBuffer
)
GLES20.glEnableVertexAttribArray(colorHandle)
```

### 绘制

```kotlin
GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
// 参数：绘制模式、起始索引、顶点数量
```

**绘制模式**：
| 模式 | 说明 |
|------|------|
| `GL_TRIANGLES` | 每三个顶点组成一个三角形 |
| `GL_TRIANGLE_STRIP` | 三角形带 |
| `GL_TRIANGLE_FAN` | 三角形扇 |

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **NDC** | 归一化设备坐标，范围 [-1, 1] |
| **顶点着色器** | 处理每个顶点的程序 |
| **片段着色器** | 计算每个像素颜色的程序 |
| **attribute** | 顶点属性变量 |
| **varying** | 顶点到片段的插值变量 |
| **光栅化** | 将图元转换为片段的过程 |
| **FloatBuffer** | Java NIO 缓冲区，用于传递顶点数据给 OpenGL |

---

## 颜色插值原理

```
顶点颜色            片段颜色（自动插值）
   红                    渐变过渡
    △                   ╱ ╲
   ╱ ╲                 ╱   ╲
  ╱   ╲               ╱  紫  ╲
 ╱     ╲             ╱   色   ╲
绿───────蓝         绿─────────蓝

中心点颜色 = (红 + 绿 + 蓝) / 3
```

OpenGL 会在光栅化阶段自动对 `varying` 变量进行线性插值。

---

## 练习

1. **修改三角形位置**：尝试修改顶点坐标，移动三角形
   - 向右移动 0.5：所有 x 值 +0.5
   - 放大两倍：所有坐标 ×2

2. **修改颜色**：尝试不同的颜色组合
   - 黄色：(1.0, 1.0, 0.0)
   - 青色：(0.0, 1.0, 1.0)
   - 紫色：(1.0, 0.0, 1.0)

3. **思考**：为什么每个顶点只有一个颜色，但整个三角形是渐变的？

---

## 常见问题

**Q: 为什么用 FloatBuffer 而不是 float[]？**
A: OpenGL 是本地库，需要直接内存访问。FloatBuffer 分配在堆外内存，可以被 OpenGL 直接读取。

**Q: gl_Position 是 vec4，但我们的位置只有 x 和 y？**
A: vec4 是，z=0（2D 图形），w=1（齐次坐标，用于透视除法）。

**Q: 为什么要调用 glEnableVertexAttribArray？**
A: 默认情况下属性是禁用的。启用后，每次绘制都会从缓冲区读取数据。

**Q: 渐变色是怎么产生的？**
A: 光栅化阶段，GPU 根据 varying 变量，在三角形内部进行线性插值计算每个片段的值。

---

## 下一步

Day 3 将学习：
- 更复杂的片段着色器
- 纹理坐标（UV）
- 给三角形贴图