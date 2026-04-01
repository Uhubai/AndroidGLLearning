# Day 5: 平移矩阵 + 动画基础

## 学习目标

- 理解平移矩阵的推导
- 实现图形平移动画
- 学习矩阵乘法基础
- 使用时间变量创建动画

## 代码结构

```
app/src/main/java/com/example/glearning/
├── MainActivity.kt       # 主 Activity，切换到 Day5Renderer
├── Day1Renderer.kt       # Day 1 渲染器
├── Day2Renderer.kt       # Day 2 渲染器
├── Day3Renderer.kt       # Day 3 渲染器
├── Day4Renderer.kt       # Day 4 渲染器
└── Day5Renderer.kt       # Day 5 渲染器（新建）
```

---

## 核心概念

### 1. 为什么需要平移矩阵？

之前的图形位置固定，无法移动。平移矩阵让我们可以：
- 改变图形位置
- 创建动画效果
- 组合多个变换

```
固定位置                    平移后
    │                          │
    ┌───┐                  ┌───┐
    │   │                  │   │
    │   │                  │   │
    └───┘                  └───┘
  原点 (0,0)             移动到 (100, 50)
```

### 2. 齐次坐标

2D 坐标 (x, y) 用 3D 齐次坐标表示：(x, y, 1)

```
┌─────────────────────────────────────────────────────────────┐
│                     齐次坐标系统                             │
└─────────────────────────────────────────────────────────────┘

普通 2D 坐标          齐次坐标
(x, y)               (x, y, w)
                      │   │   │
                      │   │   └─ w=1 表示点
                      │   └──── y 坐标
                      └─────── x 坐标

齐次坐标优势：
- 用矩阵表示平移（普通矩阵无法表示）
- 统一所有变换（平移、旋转、缩放）
- w=0 表示向量（方向），w=1 表示点（位置）
```

### 3. 平移矩阵推导

将点 (x, y, 1) 移动 (tx, ty)：

```
┌─────────────────────────────────────────────────────────────┐
│                    平移矩阵推导                              │
└─────────────────────────────────────────────────────────────┘

目标：x' = x + tx
      y' = y + ty
      z' = z + tz

使用齐次坐标：
[x', y', z', 1] = [x, y, z, 1] × 平移矩阵

平移矩阵（4×4）：
┌                                          ┐
│  1    0    0    tx                        │
│  0    1    0    ty                        │
│  0    0    1    tz                        │
│  0    0    0    1                         │
└                                          ┘

验证：
x' = x·1 + y·0 + z·0 + 1·tx = x + tx ✓
y' = x·0 + y·1 + z·0 + 1·ty = y + ty ✓
z' = x·0 + y·0 + z·1 + 1·tz = z + tz ✓
```

### 4. 矩阵乘法顺序

```
┌─────────────────────────────────────────────────────────────┐
│                    变换顺序很重要                            │
└─────────────────────────────────────────────────────────────┘

顶点坐标变换：
gl_Position = ProjectionMatrix × ViewMatrix × ModelMatrix × Vertex

矩阵乘法顺序（从右到左）：
1. 先应用 Model（模型变换）- 平移、旋转、缩放
2. 再应用 View（视图变换）- 相机位置
3. 最后应用 Projection（投影变换）- 正交/透视

示例：
v' = P × V × M × v
    └─┬──┘ └─┬──┘ └─┬──┘ └─┬─┘
      │      │      │      │
   投影   视图   模型   原顶点
```

---

## 实现步骤

### 1. 创建平移矩阵

```kotlin
private val modelMatrix = FloatArray(16)  // 模型矩阵
private val resultMatrix = FloatArray(16) // 结果矩阵

// 创建平移矩阵
android.opengl.Matrix.setIdentityM(modelMatrix, 0)  // 先重置为单位矩阵
android.opengl.Matrix.translateM(modelMatrix, 0, tx, ty, 0f)  // 应用平移
```

### 2. 组合矩阵

```kotlin
// 投影 × 模型
android.opengl.Matrix.multiplyMM(
    resultMatrix, 0,
    projectionMatrix, 0,
    modelMatrix, 0
)

// 传递给着色器
GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
```

### 3. 动画实现

使用系统时间创建平滑动画：

```kotlin
private var startTime: Long = 0

override fun onDrawFrame(gl: GL10?) {
    // 计算时间差
    val currentTime = System.currentTimeMillis()
    val elapsedSeconds = (currentTime - startTime) / 1000f
    
    // 平移距离随时间变化
    val tx = Math.sin(elapsedSeconds) * 100f  // 左右移动
    val ty = Math.cos(elapsedSeconds) * 50f   // 上下移动
    
    // 更新模型矩阵
    android.opengl.Matrix.setIdentityM(modelMatrix, 0)
    android.opengl.Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
    
    // 组合矩阵并绘制
    android.opengl.Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
    
    GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
    // ... 绘制代码
}
```

---

## 动画类型

### 1. 简谐运动（Sin/Cos）

```kotlin
// 左右摆动
val tx = Math.sin(elapsedSeconds * frequency) * amplitude

// 圆周运动
val tx = Math.cos(elapsedSeconds) * radius
val ty = Math.sin(elapsedSeconds) * radius

// 弹跳效果
val ty = Math.abs(Math.sin(elapsedSeconds)) * maxHeight
```

### 2. 线性移动

```kotlin
// 匀速移动
val tx = speed * elapsedSeconds

// 循环移动
val tx = (elapsedSeconds * speed) % maxDistance - maxDistance/2
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **齐次坐标** | 用 (x, y, w) 表示 2D 点，w=1 表示点，w=0 表示向量 |
| **模型矩阵** | 物体自身的变换（平移、旋转、缩放） |
| **视图矩阵** | 相机位置的变换 |
| **单位矩阵** | 不做任何变换的初始矩阵 |
| **三角函数** | Sin/Cos 用于周期性动画 |

---

## 矩阵操作 API

Android `android.opengl.Matrix` 类提供的方法：

| 方法 | 功能 |
|------|------|
| `setIdentityM` | 设置为单位矩阵 |
| `translateM` | 平移变换 |
| `rotateM` | 旋转变换 |
| `scaleM` | 缩放变换 |
| `multiplyMM` | 矩阵相乘 |
| `orthoM` | 正交投影矩阵 |
| `invertM` | 矩阵求逆 |

---

## 练习

1. **修改动画轨迹**：
   - 让矩形沿对角线移动
   - 实现椭圆运动轨迹
   - 添加弹跳效果

2. **控制动画速度**：
   - 修改 `frequency` 改变摆动频率
   - 修改 `amplitude` 改变移动幅度

3. **思考**：
   - 为什么矩阵乘法顺序是 P × V × M × v？
   - 如果先平移再旋转，和先旋转再平移有什么不同？

---

## 常见问题

**Q: 为什么需要齐次坐标？**
A: 普通的 2×2 或 3×3 矩阵无法表示平移，齐次坐标让所有变换统一为矩阵乘法。

**Q: translateM 和直接修改顶点坐标有什么区别？**
A: 
- translateM：在着色器中变换，顶点数据不变，适合动画
- 修改顶点：改变顶点数据，适合静态位置调整

**Q: 如何让动画更平滑？**
A: 使用 `System.nanoTime()` 获取更精确的时间，或使用插值算法。

**Q: 矩阵乘法顺序为什么是从右到左？**
A: 矩阵乘法是左结合的，P × V × M × v 表示先 M 变换，再 V，最后 P。

---

## 数学基础回顾

### 矩阵乘法规则

```
矩阵 A (m×n) × 矩阵 B (n×p) = 矩阵 C (m×p)

示例（4×4 矩阵）：
┌           ┐   ┌           ┐   ┌           ┐
│ a b c d   │   │ e f g h   │   │ ...       │
│ i j k l   │ × │ m n o p   │ = │ ...       │
│ q r s t   │   │ u v w x   │   │ ...       │
│ y z ! @   │   │ # $ % ^   │   │ ...       │
└           ┘   └           ┘   └           ┘

计算规则：
C[i,j] = Σ A[i,k] × B[k,j]  (k 从 0 到 n-1)
```

### 单位矩阵

```
┌           ┐
│ 1 0 0 0   │
│ 0 1 0 0   │
│ 0 0 1 0   │
│ 0 0 0 1   │
└           ┘

特性：I × M = M × I = M（任何矩阵乘单位矩阵等于自身）
```

---

## 下一步

Day 6 将学习：
- 旋转矩阵
- 三角函数推导
- 旋转动画实现