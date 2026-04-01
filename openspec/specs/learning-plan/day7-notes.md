# Day 7: 矩阵变换综合 - 五角星动画

## 学习目标

- 掌握复杂图形顶点计算（三角函数）
- 理解矩阵变换组合顺序
- 实现多动画效果叠加

---

## 核心概念

### 1. 五角星顶点计算

```
┌─────────────────────────────────────────────────────────────────┐
│                    五角星几何结构                                │
└─────────────────────────────────────────────────────────────────┘

五角星由 10 个顶点组成：
- 5 个外顶点（尖端）
- 5 个内顶点（凹点）

顶点分布：
        外顶点0
           │
          ╱│╲
   内顶点4─┼─内顶点0
        ╱ ╱│╲ ╲
外顶点4───╱─┼─╲───外顶点1
       ╲ ╱ │ ╲ ╱
        内─┼─内
         ╲│╱
      外顶点3───外顶点2

角度计算：
- 外顶点：每隔 72° (360° / 5)
- 内顶点：每隔 72°，偏移 36°
- 公式：angle = i × 72° (外顶点)
        angle = i × 72° + 36° (内顶点)
```

### 2. 三角函数坐标计算

```
┌─────────────────────────────────────────────────────────────────┐
│                    极坐标转换为直角坐标                          │
└─────────────────────────────────────────────────────────────────┘

已知：半径 r，角度 θ
计算：x = r × cos(θ)
      y = r × sin(θ)

五角星顶点公式：
外顶点 i：
  angle = i × 72° = i × (2π / 5)
  x = outerRadius × cos(angle)
  y = outerRadius × sin(angle)

内顶点 i：
  angle = i × 72° + 36° = i × (2π / 5) + π / 10
  x = innerRadius × cos(angle)
  y = innerRadius × sin(angle)

代码实现：
for (i in 0..4) {
    // 外顶点
    val outerAngle = i * 72f * (PI / 180f)
    outerX = outerRadius * cos(outerAngle)
    outerY = outerRadius * sin(outerAngle)
    
    // 内顶点
    val innerAngle = i * 72f + 36f * (PI / 180f)
    innerX = innerRadius * cos(innerAngle)
    innerY = innerRadius * sin(innerAngle)
}
```

### 3. 索引缓冲布局

```
┌─────────────────────────────────────────────────────────────────┐
│                    五角星三角形组成                              │
└─────────────────────────────────────────────────────────────────┘

五角星由 10 个三角形组成：
- 5 个外三角形（尖端）
- 5 个内三角形（凹处连接）

顶点顺序（交替存储）：
[外0, 内0, 外1, 内1, 外2, 内2, 外3, 内3, 外4, 内4]
索引：0,  1,   2,   3,   4,   5,   6,   7,   8,   9

三角形索引：
三角形0: [外0, 内0, 外1] → [0, 1, 2]
三角形1: [外1, 内1, 外2] → [2, 3, 4]
三角形2: [外2, 内2, 外3] → [4, 5, 6]
三角形3: [外3, 内3, 外4] → [6, 7, 8]
三角形4: [外4, 内4, 外0] → [8, 9, 0]
三角形5: [内0, 外0, 内4] → [1, 0, 9]  (中心连接)
三角形6: [内0, 内4, 内1] → [1, 9, 3]  (中心填充)
三角形7: [内1, 内4, 内2] → [3, 9, 5]  ...需要简化

简化方案：5 个三角形即可（尖端）：
INDICES = [0, 1, 2, 2, 3, 4, 4, 5, 6, 6, 7, 8, 8, 9, 0]
          └─尖端0─┘ └─尖端1─┘ └─尖端2─┘ └─尖端3─┘ └─尖端4─┘
```

### 4. 矩阵变换组合

```
┌─────────────────────────────────────────────────────────────────┐
│                    变换顺序与效果                                │
└─────────────────────────────────────────────────────────────────┘

矩阵乘法顺序：从右到左执行

结果 = Projection × Translation × Rotation × Scale × Vertex

变换顺序对效果的影响：

顺序1：缩放 → 旋转 → 平移
  效果：五角星原地缩放 → 原地旋转 → 移动位置
  ✓ 推荐：符合直觉

顺序2：平移 → 旋转 → 缩放
  效果：五角星移动 → 绕原点旋转（轨迹变圆）→ 缩放
  ✗ 不推荐：旋转轴偏离中心

代码实现：
Matrix.setIdentityM(modelMatrix, 0)
Matrix.scaleM(modelMatrix, 0, sx, sy, 1)      // 先缩放
Matrix.rotateM(modelMatrix, 0, angle, 0, 0, 1) // 再旋转
Matrix.translateM(modelMatrix, 0, tx, ty, 0)   // 最后平移

组合矩阵：
Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
```

### 5. 多动画组合

```
┌─────────────────────────────────────────────────────────────────┐
│                    动画参数设计                                  │
└─────────────────────────────────────────────────────────────────┘

旋转动画：
  angle = elapsedSeconds × 60  // 每秒旋转 60 度
  angle = angle % 360          // 限制在 0-360 度范围

缩放动画：
  scale = 1.0 + sin(elapsedSeconds × 2) × 0.3  // 周期性缩放
  范围：[0.7, 1.3]，频率 2 Hz

平移动画：
  tx = sin(elapsedSeconds × 1.5) × 50  // 左右移动
  ty = cos(elapsedSeconds × 2) × 30   // 上下移动

组合效果：
  五角星在移动过程中旋转并缩放
  各动画频率不同，产生复杂轨迹
```

---

## 实现步骤

### 1. 计算顶点坐标

```kotlin
private fun generateStarVertices(outerRadius: Float, innerRadius: Float): FloatArray {
    val vertices = FloatArray(10 * 5)  // 10顶点 × (2坐标 + 3颜色)
    
    for (i in 0..4) {
        // 外顶点（5个尖端）
        val outerAngle = i * 72f * (PI / 180f)
        val outerX = outerRadius * cos(outerAngle).toFloat()
        val outerY = outerRadius * sin(outerAngle).toFloat()
        
        // 内顶点（5个凹点）
        val innerAngle = (i * 72f + 36f) * (PI / 180f)
        val innerX = innerRadius * cos(innerAngle).toFloat()
        val innerY = innerRadius * sin(innerAngle).toFloat()
        
        // 存储顶点数据（交错格式）
        vertices[i * 2 * 5] = outerX      // 外顶点 x
        vertices[i * 2 * 5 + 1] = outerY  // 外顶点 y
        vertices[i * 2 * 5 + 2] = ...     // 外顶点颜色
        
        vertices[i * 2 * 5 + 5] = innerX  // 内顶点 x
        vertices[i * 2 * 5 + 6] = innerY  // 内顶点 y
        vertices[i * 2 * 5 + 7] = ...     // 内顶点颜色
    }
    
    return vertices
}
```

### 2. 设置颜色

```kotlin
// 五角星五个尖端各有不同颜色
val colors = arrayOf(
    floatArrayOf(1.0f, 0.0f, 0.0f),  // 红色
    floatArrayOf(0.0f, 1.0f, 0.0f),  // 绿色
    floatArrayOf(0.0f, 0.0f, 1.0f),  // 蓝色
    floatArrayOf(1.0f, 0.0f, 1.0f),  // 紫色
    floatArrayOf(1.0f, 1.0f, 0.0f)   // 黄色
)

// 内顶点使用相邻外顶点的颜色混合
```

### 3. 矩阵变换

```kotlin
override fun onDrawFrame(gl: GL10?) {
    val elapsedSeconds = (currentTime - startTime) / 1000f
    
    // 计算动画参数
    val angle = elapsedSeconds * 60f
    val scale = 1.0f + sin(elapsedSeconds * 2.0).toFloat() * 0.3f
    val tx = sin(elapsedSeconds * 1.5).toFloat() * 50f
    val ty = cos(elapsedSeconds * 2.0).toFloat() * 30f
    
    // 组合矩阵变换（顺序：缩放 → 旋转 → 平移）
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.scaleM(modelMatrix, 0, scale, scale, 1f)
    Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)
    Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
    
    Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
    
    // 绘制...
}
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **极坐标** | 用角度和半径表示位置，(r, θ) |
| **直角坐标** | 用 x, y 表示位置 |
| **三角函数** | sin/cos 用于角度和坐标转换 |
| **变换顺序** | 矩阵乘法从右到左执行 |
| **复合变换** | 多个变换矩阵的组合 |

---

## 常见问题

**Q: 为什么外顶点和内顶点交替存储？**
A: 便于使用索引缓冲绘制，减少索引复杂度。

**Q: 变换顺序为什么是缩放→旋转→平移？**
A: 先缩放保证物体大小变化正确，再旋转保证旋转轴在物体中心，最后平移移动位置。

**Q: 五角星为什么用 5 个三角形而不是 10 个？**
A: 5 个尖端三角形已能形成五角星轮廓，内凹处无需填充。

---

## 下一步

Day 8 将学习：
- 旋转矩阵数学推导
- rotateM API 使用
- 原地旋转动画