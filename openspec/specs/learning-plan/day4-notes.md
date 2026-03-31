# Day 4: 正交投影 + 屏幕适配

## 学习目标

- 理解正交投影矩阵
- 实现屏幕适配（保持图形比例）
- 使用 `uniform` 变量传递矩阵

## 代码结构

```
app/src/main/java/com/example/glearning/
├── MainActivity.kt       # 主 Activity，切换到 Day4Renderer
├── Day1Renderer.kt       # Day 1 渲染器
├── Day2Renderer.kt       # Day 2 渲染器
├── Day3Renderer.kt       # Day 3 渲染器
└── Day4Renderer.kt       # Day 4 渲染器（新建）
```

---

## 核心概念

### 1. 为什么需要正交投影？

之前的坐标使用 NDC [-1, 1]，在不同屏幕比例下图形会变形：

```
正方形在 NDC 中：           在宽屏手机上：

   ┌───┐                      ┌─────────┐
   │   │                      │         │
   │   │                      │         │
   └───┘                      └─────────┘
  (正方形)                    (被拉伸成矩形)
```

正交投影矩阵将坐标映射到视口，保持比例不变。

### 2. 正交投影矩阵

正交投影将 3D 空间的一个矩形区域映射到 NDC：

```
┌─────────────────────────────────────────────────────────────┐
│                    正交投影映射                              │
└─────────────────────────────────────────────────────────────┘

世界坐标                      NDC 坐标
   │                            │
   │  左边界 left               │  -1
   │  右边界 right              │  +1
   │  下边界 bottom             │  -1
   │  上边界 top                │  +1
   │                            │
   └───────映射───────────────▶│

公式：
x_ndc = (x - left) / (right - left) * 2 - 1
y_ndc = (y - bottom) / (top - bottom) * 2 - 1
z_ndc = (z - near) / (far - near) * 2 - 1
```

### 3. 矩阵推导

正交投影矩阵（4×4）：

```
┌───────────────────────────────────────────────────────────┐
│                    正交投影矩阵                            │
└───────────────────────────────────────────────────────────┘

     ┌                                          ┐
     │  2/(r-l)    0         0       -(r+l)/(r-l) │
     │    0      2/(t-b)     0       -(t+b)/(t-b) │
     │    0        0      2/(f-n)    -(f+n)/(f-n) │
     │    0        0         0            1       │
     └                                          ┘

其中：
- l = left（左边界）
- r = right（右边界）
- b = bottom（下边界）
- t = top（上边界）
- n = near（近裁剪面）
- f = far（远裁剪面）
```

### 4. 屏幕适配策略

```kotlin
val aspectRatio = width.toFloat() / height.toFloat()

if (aspectRatio > 1f) {
    // 宽屏：扩展水平范围
    Matrix.orthoM(projectionMatrix, 0,
        -aspectRatio, aspectRatio,  // left, right
        -1f, 1f,                     // bottom, top
        -1f, 1f                      // near, far
    )
} else {
    // 竖屏：扩展垂直范围
    Matrix.orthoM(projectionMatrix, 0,
        -1f, 1f,                     // left, right
        -1f/aspectRatio, 1f/aspectRatio,  // bottom, top
        -1f, 1f                      // near, far
    )
}
```

**效果**：
- 无论屏幕比例如何，100×100 的矩形始终显示为正方形
- 世界坐标范围随屏幕比例调整

---

## 着色器变化

### 顶点着色器

```glsl
uniform mat4 u_Matrix;      // 投影矩阵（新增）
attribute vec4 a_Position;
attribute vec4 a_Color;
varying vec4 v_Color;

void main() {
    gl_Position = u_Matrix * a_Position;  // 应用矩阵
    v_Color = a_Color;
}
```

**uniform vs attribute**：
| 类型 | 说明 | 更新频率 |
|------|------|----------|
| `attribute` | 顶点属性 | 每个顶点不同 |
| `uniform` | 全局常量 | 所有顶点共享 |
| `varying` | 插值变量 | 传递给片段着色器 |

### 传递矩阵

```kotlin
matrixHandle = GLES20.glGetUniformLocation(program, "u_Matrix")
GLES20.glUniformMatrix4fv(matrixHandle, 1, false, projectionMatrix, 0)
```

---

## 坐标系统对比

```
┌─────────────────────────────────────────────────────────────┐
│                    坐标系统演进                              │
└─────────────────────────────────────────────────────────────┘

Day 1-3: NDC 坐标 [-1, 1]
   - 图形会随屏幕比例变形
   - 坐标固定，难以控制

Day 4: 世界坐标 + 投影
   - 使用像素级坐标（如 -100 到 100）
   - 正交投影自动适配屏幕
   - 图形比例保持不变

示例：
   矩形顶点：(-100, 100), (-100, -100), (100, 100), (100, -100)
   无论屏幕尺寸，始终显示为 200×200 的正方形
```

---

## 关键代码解析

### 创建投影矩阵

```kotlin
private val projectionMatrix = FloatArray(16)  // 4×4 矩阵

override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    GLES20.glViewport(0, 0, width, height)
    
    val aspectRatio = width.toFloat() / height.toFloat()
    
    if (aspectRatio > 1f) {
        Matrix.orthoM(projectionMatrix, 0,
            -aspectRatio, aspectRatio, -1f, 1f, -1f, 1f)
    } else {
        Matrix.orthoM(projectionMatrix, 0,
            -1f, 1f, -1f/aspectRatio, 1f/aspectRatio, -1f, 1f)
    }
}
```

### 使用像素坐标

```kotlin
private val RECT_COORDS_AND_COLORS = floatArrayOf(
    -100f,  100f,  1.0f, 0.0f, 0.0f,  // 左上 (像素坐标)
    -100f, -100f,  0.0f, 1.0f, 0.0f,  // 左下
     100f,  100f,  0.0f, 0.0f, 1.0f,  // 右上
     100f, -100f,  1.0f, 1.0f, 0.0f   // 右下
)
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **正交投影** | 无透视的平行投影，保持比例 |
| **透视投影** | 有深度的投影，远处物体变小 |
| **aspectRatio** | 宽高比，width / height |
| **uniform** | 着色器全局常量 |
| **Matrix.orthoM** | Android 矩阵工具类 |

---

## 矩阵布局

OpenGL 矩阵使用列主序（Column-Major）：

```
索引位置：
┌           ┐
│ 0  4  8 12│
│ 1  5  9 13│
│ 2  6 10 14│
│ 3  7 11 15│
└           ┘

FloatArray[16] 存储顺序：
[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15]
```

---

## 练习

1. **修改矩形大小**：
   - 改为 50×50：坐标改为 ±50
   - 改为 300×300：坐标改为 ±300

2. **绘制多个矩形**：
   - 在不同位置绘制多个矩形
   - 观察是否都保持正方形比例

3. **思考**：
   - 为什么正交投影更适合 2D 图形？
   - 透视投影何时需要使用？

---

## 常见问题

**Q: orthoM 参数的含义是什么？**
A: `orthoM(matrix, offset, left, right, bottom, top, near, far)`
   - left/right：水平范围
   - bottom/top：垂直范围
   - near/far：深度范围（2D 可设为 -1, 1）

**Q: 为什么使用 Android 的 Matrix 类？**
A: 简化矩阵计算，避免手动推导。也可以自己实现或使用其他库。

**Q: uniform 和 attribute 的区别？**
A: 
- uniform：所有顶点共享，如矩阵、光照参数
- attribute：每个顶点不同，如位置、颜色

**Q: 屏幕旋转时图形会变形吗？**
A: 不会。`onSurfaceChanged` 在旋转时重新计算投影矩阵。

---

## 下一步

Day 5 将学习：
- 相机视图矩阵
- 模型变换矩阵
- MVP 矩阵组合