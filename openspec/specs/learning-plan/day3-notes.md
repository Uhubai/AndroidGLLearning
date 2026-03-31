# Day 3: VBO + 索引缓冲绘制矩形

## 学习目标

- 理解 VBO（Vertex Buffer Object）
- 使用索引缓冲（Index Buffer）避免重复顶点
- 学习 `glDrawElements` 绘制方法
- 理解 `GL_TRIANGLE_STRIP` vs `GL_TRIANGLES`

## 代码结构

```
app/src/main/java/com/example/glearning/
├── MainActivity.kt       # 主 Activity，切换到 Day3Renderer
├── Day1Renderer.kt       # Day 1 渲染器
├── Day2Renderer.kt       # Day 2 渲染器
└── Day3Renderer.kt       # Day 3 渲染器（新建）
```

---

## 核心概念

### 1. 为什么需要索引缓冲？

绘制矩形需要 2 个三角形，共 6 个顶点。但矩形只有 4 个角点：

```
不使用索引（6 个顶点，有重复）：
   0──────2
   │╲     │
   │ ╲    │
   │  ╲   │
   │   ╲  │
   │    ╲ │
   1──────3

顶点数据：0, 1, 2, 1, 3, 2 （顶点 1 和 2 重复）

使用索引（4 个顶点 + 6 个索引）：
顶点数据：0, 1, 2, 3（4 个唯一顶点）
索引数据：[0, 1, 2, 1, 3, 2]（引用顶点）
```

**优势**：
- 减少内存占用（4 个顶点 vs 6 个顶点）
- 提高渲染效率
- 复杂模型节省更多

### 2. 矩形顶点布局

```kotlin
private val RECT_COORDS_AND_COLORS = floatArrayOf(
    // 位置         颜色 (RGB)
    -0.5f,  0.5f,   1.0f, 0.0f, 0.0f,  // 左上 - 红
    -0.5f, -0.5f,   0.0f, 1.0f, 0.0f,  // 左下 - 绿
     0.5f,  0.5f,   0.0f, 0.0f, 1.0f,  // 右上 - 蓝
     0.5f, -0.5f,   1.0f, 1.0f, 0.0f   // 右下 - 黄
)

private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)
```

### 3. 索引绘制流程

```
┌───────────────────────────────────────────────────────────────┐
│                        绘制流程                                │
└───────────────────────────────────────────────────────────────┘

顶点缓冲                        索引缓冲
   │                              │
   │  顶点 0: (-0.5, 0.5, 红)     │  索引 0 → 顶点 0
   │  顶点 1: (-0.5,-0.5, 绿)     │  索引 1 → 顶点 1
   │  顶点 2: ( 0.5, 0.5, 蓝)     │  細引 2 → 顶点 2
   │  顶点 3: ( 0.5,-0.5, 黄)     │  ...
   │                              │
   └──────────────┬───────────────┘
                  │
                  ▼
            ┌───────────┐
            │ glDrawElements │
            │ 按索引顺序组装 │
            │ 三角形       │
            └───────────┘
                  │
                  ▼
            三角形 1: (0, 1, 2)
            三角形 2: (1, 3, 2)
```

### 4. GL_TRIANGLES vs GL_TRIANGLE_STRIP

| 模式 | 说明 | 索引数 | 适用场景 |
|------|------|--------|----------|
| `GL_TRIANGLES` | 每 3 个索引组成一个三角形 | 6 | 独立三角形，灵活 |
| `GL_TRIANGLE_STRIP` | 连续三角形带 | 4 | 连续三角形，高效 |

**GL_TRIANGLE_STRIP 示例**：
```
索引: 0, 1, 2, 3

三角形 1: (0, 1, 2)
三角形 2: (1, 2, 3) ← 自动翻转顺序为 (2, 1, 3)

   0──────2
   │╲   ╱│
   │ ╲ ╱ │
   │  ╲  │
   │ ╱ ╲ │
   │╱   ╲│
   1──────3
```

---

## 关键代码解析

### 创建索引缓冲

```kotlin
private val RECT_INDICES = shortArrayOf(0, 1, 2, 1, 3, 2)

// 创建 ShortBuffer
val ib = ByteBuffer.allocateDirect(RECT_INDICES.size * SHORT_SIZE)
ib.order(ByteOrder.nativeOrder())
indexBuffer = ib.asShortBuffer()
indexBuffer?.put(RECT_INDICES)
indexBuffer?.position(0)
```

**注意**：
- 使用 `short` 类型（节省内存）
- `GL_UNSIGNED_SHORT` 匹配

### 索引绘制

```kotlin
GLES20.glDrawElements(
    GLES20.GL_TRIANGLES,      // 绘制模式
    RECT_INDICES.size,        // 索引数量
    GLES20.GL_UNSIGNED_SHORT, // 索引数据类型
    indexBuffer               // 索引缓冲
)
```

**参数说明**：
| 参数 | 含义 |
|------|------|
| mode | 绘制模式 |
| count | 索引数量 |
| type | 索引数据类型 |
| indices | 索引缓冲指针 |

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **VBO** | Vertex Buffer Object，存储顶点数据 |
| **IBO/EBO** | Index/Element Buffer Object，存储索引 |
| **glDrawElements** | 通过索引绘制图元 |
| **glDrawArrays** | 直接绘制（无索引） |
| **索引顺序** | 影响三角形正反面（顺时针/逆时针） |

---

## 三角形正反面

OpenGL 默认：
- **正面**：逆时针（CCW）顶点顺序
- **反面**：顺时针（CW）顶点顺序

```
逆时针（正面）              顺时针（反面）
   0                        0
   │╲                       │╱
   │ ╲                      │ ╲
   │  ╲                     │  ╲
   1───2                    2───1
   
索引: (0, 1, 2) ✓           索引: (0, 2, 1) ✗
```

**影响**：
- 背面剔除（`glEnable(GL_CULL_FACE)`）时，反面不绘制
- 正确排序避免渲染问题

---

## 练习

1. **使用 GL_TRIANGLE_STRIP**：
   - 修改索引为 `[0, 1, 2, 3]`
   - 修改绘制模式为 `GL_TRIANGLE_STRIP`
   - 观察效果是否相同

2. **添加更多顶点**：
   - 绘制两个相邻的矩形
   - 尝试共享边上的顶点

3. **思考**：
   - 为什么索引使用 `short` 而不是 `int`？
   - 什么情况下需要使用 `int` 类型索引？

---

## 常见问题

**Q: glDrawArrays 和 glDrawElements 有什么区别？**
A: 
- `glDrawArrays`：直接按顶点顺序绘制，顶点可能重复
- `glDrawElements`：通过索引引用顶点，避免重复

**Q: 为什么索引用 short？**
A: short 节省内存，支持最多 65535 个顶点。超过时需用 int。

**Q: 矩形需要多少个索引？**
A: 使用 `GL_TRIANGLES` 需要 6 个（2 个三角形 × 3 个顶点）。
   使用 `GL_TRIANGLE_STRIP` 只需要 4 个。

**Q: 索引顺序重要吗？**
A: 重要！顺序决定三角形正反面，影响背面剔除和光照计算。

---

## 下一步

Day 4 将学习：
- 正交投影矩阵
- 屏幕适配
- 绘制等比例图形