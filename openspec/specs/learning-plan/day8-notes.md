# Day 8: 旋转矩阵 - 原地旋转动画

## 学习目标

- 理解旋转矩阵的数学推导
- 掌握三角函数在旋转中的应用
- 实现原地旋转动画效果

---

## 核心概念

### 1. 旋转的几何原理

```
┌─────────────────────────────────────────────────────────────────┐
│                    旋转的几何意义                                │
└─────────────────────────────────────────────────────────────────┘

点 P(x, y) 绕原点旋转角度 θ 后到达 P'(x', y')

        y轴
         │
         │    P'(新位置)
         │   ╱
         │ ╱ θ
         │╱──── P(原位置)
─────────┼─────────── x轴
        O(原点)

旋转方向：
- θ > 0：逆时针旋转（正面）
- θ < 0：顺时针旋转（背面）
```

### 2. 旋转公式推导

```
┌─────────────────────────────────────────────────────────────────┐
│                    三角函数推导旋转公式                          │
└─────────────────────────────────────────────────────────────────┘

设点 P 的极坐标为 (r, α)，则：
  x = r × cos(α)
  y = r × sin(α)

旋转 θ 后，极坐标变为 (r, α + θ)：
  x' = r × cos(α + θ)
  y' = r × sin(α + θ)

使用三角函数展开：
  cos(α + θ) = cos(α) × cos(θ) - sin(α) × sin(θ)
  sin(α + θ) = sin(α) × cos(θ) + cos(α) × sin(θ)

代入：
  x' = r × [cos(α) × cos(θ) - sin(α) × sin(θ)]
     = r × cos(α) × cos(θ) - r × sin(α) × sin(θ)
     = x × cos(θ) - y × sin(θ)

  y' = r × [sin(α) × cos(θ) + cos(α) × sin(θ)]
     = r × sin(α) × cos(θ) + r × cos(α) × sin(θ)
     = y × cos(θ) + x × sin(θ)

结论：
  x' = x × cos(θ) - y × sin(θ)
  y' = x × sin(θ) + y × cos(θ)
```

### 3. 旋转矩阵形式

```
┌─────────────────────────────────────────────────────────────────┐
│                    2D 旋转矩阵                                   │
└─────────────────────────────────────────────────────────────────┘

将旋转公式写成矩阵形式：

[x']   [cos(θ)  -sin(θ)]   [x]
[y'] = [sin(θ)   cos(θ)] × [y]

扩展为 3×3 齐次坐标矩阵（用于 2D）：

┌                           ┐
│ cos(θ)  -sin(θ)    0      │
│ sin(θ)   cos(θ)    0      │
│ 0        0         1      │
└                           ┘

扩展为 4×4 齐次坐标矩阵（用于 3D，绕 Z 轴旋转）：

┌                              ┐
│ cos(θ)  -sin(θ)  0    0      │
│ sin(θ)   cos(θ)  0    0      │
│ 0        0       1    0      │
│ 0        0       0    1      │
└                              ┘

旋转轴：
- 绕 Z 轴：(0, 0, 1) - 平面旋转，z 不变
- 绕 X 轴：(1, 0, 0) - x 不变
- 绕 Y 轴：(0, 1, 0) - y 不变
```

### 4. 绕不同轴旋转的矩阵

```
┌─────────────────────────────────────────────────────────────────┐
│                    3D 旋转矩阵（绕各轴）                         │
└─────────────────────────────────────────────────────────────────┘

绕 Z 轴旋转（平面旋转）：
┌                              ┐
│ cos(θ)  -sin(θ)  0    0      │
│ sin(θ)   cos(θ)  0    0      │
│ 0        0       1    0      │
│ 0        0       0    1      │
└                              ┘

绕 X 轴旋转：
┌                              ┐
│ 1    0         0        0    │
│ 0    cos(θ)  -sin(θ)   0     │
│ 0    sin(θ)   cos(θ)   0     │
│ 0    0         0        1    │
└                              ┘

绕 Y 轴旋转：
┌                              ┐
│ cos(θ)   0    sin(θ)   0     │
│ 0        1    0        0     │
│ -sin(θ)  0    cos(θ)   0     │
│ 0        0    0        1     │
└                              ┘

绕任意轴旋转：
- 需使用 Rodrigues 旋转公式
- Android Matrix.rotateM 自动处理
```

### 5. rotateM API 使用

```
┌─────────────────────────────────────────────────────────────────┐
│                    Android Matrix.rotateM                       │
└─────────────────────────────────────────────────────────────────┘

API：
Matrix.rotateM(matrix, offset, angle, axisX, axisY, axisZ)

参数说明：
- matrix: 目标矩阵（4×4 = 16 个浮点数）
- offset: 矩阵数据起始偏移（通常为 0）
- angle: 旋转角度（度数，非弧度）
- axisX, axisY, axisZ: 旋转轴向量

示例：
// 绕 Z 轴旋转 45 度
Matrix.rotateM(modelMatrix, 0, 45f, 0f, 0f, 1f)

// 绕 X 轴旋转 30 度
Matrix.rotateM(modelMatrix, 0, 30f, 1f, 0f, 0f)

// 绕 Y 轴旋转 60 度
Matrix.rotateM(modelMatrix, 0, 60f, 0f, 1f, 0f)

注意：
- angle 单位是度（degree），不是弧度（radian）
- 旋转轴向量需要归一化（长度为 1）
- 正角度为逆时针旋转
```

### 6. 旋转动画实现

```
┌─────────────────────────────────────────────────────────────────┐
│                    基于时间的旋转动画                            │
└─────────────────────────────────────────────────────────────────┘

核心代码：
val elapsedSeconds = (currentTime - startTime) / 1000f
val angle = elapsedSeconds × speed

Matrix.setIdentityM(modelMatrix, 0)
Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)

动画参数：
- speed: 旋转速度（度/秒）
  - speed = 60 → 每秒旋转 60 度
  - speed = 360 → 每秒旋转一圈

连续旋转：
- angle 会持续增大
- 不需要归一化到 [0, 360]
- Matrix.rotateM 自动处理大角度

组合其他变换：
Matrix.setIdentityM(modelMatrix, 0)
Matrix.translateM(modelMatrix, 0, tx, ty, 0)  // 先平移
Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)  // 再旋转
// 结果：物体绕原点旋转（形成圆周轨迹）

原地旋转（推荐顺序）：
Matrix.setIdentityM(modelMatrix, 0)
Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)  // 先旋转
Matrix.translateM(modelMatrix, 0, tx, ty, 0)  // 再平移
// 结果：物体先原地旋转，再移动位置
```

---

## 实现步骤

### 1. 矩形原地旋转

```kotlin
override fun onDrawFrame(gl: GL10?) {
    // 计算旋转角度
    val elapsedSeconds = (currentTime - startTime) / 1000f
    val angle = elapsedSeconds * 60f  // 每秒 60 度
    
    // 组合矩阵
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)  // 绕 Z 轴
    
    Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
    
    // 绘制...
}
```

### 2. 旋转 + 平移组合

```kotlin
// 方式1：原地旋转后移动
Matrix.setIdentityM(modelMatrix, 0)
Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)
Matrix.translateM(modelMatrix, 0, tx, ty, 0)

// 方式2：移动后绕原点旋转（轨道运动）
Matrix.setIdentityM(modelMatrix, 0)
Matrix.translateM(modelMatrix, 0, tx, ty, 0)
Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **弧度** | 角度的数学单位，π 弧度 = 180 度 |
| **度** | 常用角度单位，一圈 = 360 度 |
| **旋转轴** | 旋转围绕的直线方向 |
| **齐次坐标** | 用 n+1 维表示 n 维坐标，便于矩阵运算 |
| **逆时针** | 正角度旋转方向（正面） |

---

## 常见问题

**Q: 为什么角度单位是度而不是弧度？**
A: Android Matrix API 使用度数，更直观易用。数学公式推导时用弧度。

**Q: 旋转角度会无限增大吗？**
A: 是的，但 Matrix.rotateM 可以处理任意角度。大角度不影响正确性。

**Q: 如何让物体绕自身中心旋转而非原点？**
A: 先旋转（物体在原点），再平移到目标位置。

**Q: 如何实现轨道旋转（卫星运动）？**
A: 先平移到轨道位置，再绕原点旋转。

---

## 数学公式速查

```
弧度与度转换：
  弧度 = 度 × (π / 180)
  度 = 弧度 × (180 / π)

常用角度：
  45° = π/4 ≈ 0.785
  90° = π/2 ≈ 1.571
  180° = π ≈ 3.142
  360° = 2π ≈ 6.283

三角函数值：
  cos(0°) = 1, sin(0°) = 0
  cos(90°) = 0, sin(90°) = 1
  cos(180°) = -1, sin(180°) = 0
  cos(360°) = 1, sin(360°) = 0
```

---

## 下一步

Day 9 将学习：
- 缩放矩阵
- 组合变换 MVP
- 复杂动画效果