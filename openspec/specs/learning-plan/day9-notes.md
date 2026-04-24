# Day 9: MVP 矩阵组合

## 学习目标

- 理解 MVP 三个矩阵各自的职责
- 掌握矩阵乘法顺序的重要性
- 实现完整的 MVP 变换管线
- 学习相机视图矩阵概念

---

## 核心概念

### 1. 什么是 MVP？

MVP 是三个矩阵的缩写：

```
┌─────────────────────────────────────────────────────────────────┐
│                    MVP 矩阵管线                                  │
└─────────────────────────────────────────────────────────────────┘

Model（模型矩阵）        View（视图矩阵）        Projection（投影矩阵）
     │                       │                        │
     ▼                       ▼                        ▼
物体自身变换             相机位置变换              可视范围定义
- 平移                   - 相机在哪里              - 正交/透视
- 旋转                   - 相机看哪里              - 裁剪面
- 缩放                   - 相机朝向                - 宽高比

最终变换：gl_Position = Projection × View × Model × Vertex
```

### 2. 三个矩阵的职责

#### Model 矩阵（模型矩阵）

```
作用：将物体从局部坐标变换到世界坐标
示例：一个矩形绕自身中心旋转、移动位置

局部坐标                  世界坐标
  ┌───┐                  原点移动 (100, 0)
  │   │                         ┌───┐
  └───┘                         │   │
(0,0 中心)                    (100, 0 中心)
```

#### View 矩阵（视图矩阵）

```
作用：将世界坐标变换到相机坐标（观察坐标）
概念：相当于移动整个世界，让相机在原点

世界坐标                  相机坐标
相机在 (0, 100)           世界下移 100
  相机                        原点 (0,0)
    │                          ┌───┐
    ▼                          │   │
  ┌───┐                        
  │   │                        
                                 
```

#### Projection 矩阵（投影矩阵）

```
作用：将可视范围映射到 NDC [-1, 1]
类型：
- 正交投影：无透视，适合 2D
- 透视投影：近大远小，适合 3D

相机坐标                  NDC 坐标
  ┌───┐                     ┌───┐
  │   │                     │   │
  └───┘                     └───┘
(世界坐标)                [-1, 1] 范围
```

### 3. 矩阵乘法顺序

```
┌─────────────────────────────────────────────────────────────────┐
│              变换顺序：从右到左执行                               │
└─────────────────────────────────────────────────────────────────┘

gl_Position = P × V × M × v

执行顺序：
1. v（原始顶点）首先应用 M（模型变换）
2. 结果应用 V（视图变换）
3. 结果应用 P（投影变换）
4. 最终得到 gl_Position

为什么是从右到左？
- 矩阵乘法是左结合的
- P × (V × (M × v))
- 先计算最内层的 M × v
```

### 4. 常见的顺序错误

```
错误：先平移再旋转
正确：先旋转再平移

示例：让物体绕原点公转
- 正确：rotateM → translateM
  先旋转（绕自身中心），再平移（移动位置）
  结果：物体沿圆形轨迹移动

- 错误：translateM → rotateM
  先平移（移动位置），再旋转（绕新位置中心）
  结果：物体在原地旋转，不会公转
```

### 5. 视图矩阵推导

```
┌─────────────────────────────────────────────────────────────────┐
│              setLookAtM 参数说明                                 │
└─────────────────────────────────────────────────────────────────┘

Matrix.setLookAtM(
    viewMatrix, 0,      // 目标矩阵和偏移
    eyeX, eyeY, eyeZ,   // 相机位置（眼睛）
    centerX, centerY, centerZ,  // 观察目标点（看哪里）
    upX, upY, upZ       // 上方向（哪个方向是上）
)

2D 示例：
- 相机位置：(0, 0, 3)   在屏幕外
- 观察目标：(0, 0, 0)   看原点
- 上方向：   (0, 1, 0)   Y 轴朝上

效果：相当于相机后退 3 个单位，看着原点
```

---

## 实现步骤

### 1. 创建 MVP 三个矩阵

```kotlin
// 三个独立的矩阵
private val modelMatrix = FloatArray(16)    // 模型矩阵
private val viewMatrix = FloatArray(16)     // 视图矩阵
private val projectionMatrix = FloatArray(16)  // 投影矩阵
private val resultMatrix = FloatArray(16)   // 最终结果
private val tempMatrix = FloatArray(16)     // 临时矩阵（用于中间计算）
```

### 2. 初始化视图矩阵

```kotlin
override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    // 设置视图矩阵（相机位置）
    Matrix.setLookAtM(
        viewMatrix, 0,
        0f, 0f, 3f,   // 相机在 (0, 0, 3)
        0f, 0f, 0f,   // 看向原点
        0f, 1f, 0f    // Y 轴朝上
    )
}
```

### 3. 每帧组合 MVP

```kotlin
override fun onDrawFrame(gl: GL10?) {
    // 1. 模型变换：旋转 + 平移
    Matrix.setIdentityM(modelMatrix, 0)
    Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)
    Matrix.translateM(modelMatrix, 0, tx, ty, 0f)
    
    // 2. 计算 View × Model
    Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
    
    // 3. 计算 Projection × (View × Model)
    Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
    
    // 4. 传递给着色器
    GLES20.glUniformMatrix4fv(matrixHandle, 1, false, resultMatrix, 0)
    
    // 5. 绘制
    GLES20.glDrawElements(...)
}
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **Model 矩阵** | 物体自身的变换（平移、旋转、缩放） |
| **View 矩阵** | 相机位置和朝向的变换 |
| **Projection 矩阵** | 定义可视范围和投影类型 |
| **MVP** | 三个矩阵的统称，完整的变换管线 |
| **世界坐标** | 物体在全局空间中的位置 |
| **观察坐标** | 相对于相机的位置 |
| **NDC** | 归一化设备坐标 [-1, 1] |

---

## 矩阵组合公式

```
最终矩阵 = Projection × View × Model

中间步骤：
tempMatrix = View × Model
resultMatrix = Projection × tempMatrix

或者：
resultMatrix = Projection × View × Model
             = (Projection × View) × Model
```

---

## 练习

1. **修改相机位置**：
   - 将相机移远：eyeZ = 5
   - 观察图形是否变小（透视效果）

2. **组合多个物体**：
   - 绘制两个矩形，各自有不同的 Model 矩阵
   - 一个旋转，一个平移

3. **思考**：
   - View 矩阵和 Model 矩阵的区别？
   - 为什么需要三个矩阵而不是一个？

---

## 常见问题

**Q: MVP 三个矩阵必须分开吗？**
A: 可以合并成一个矩阵，但分开更灵活。模型矩阵每帧变化，视图矩阵通常不变，投影矩阵只在屏幕变化时更新。

**Q: 矩阵乘法顺序反了会怎样？**
A: 变换效果完全不同。例如先平移再旋转会变成绕新位置旋转，而不是绕原点公转。

**Q: 2D 游戏需要 View 矩阵吗？**
A: 可以不需要。如果相机固定在 (0, 0, 3) 看向原点，View 矩阵是单位矩阵，可以省略。

**Q: 如何优化 MVP 计算？**
A: View 矩阵在 onSurfaceCreated 初始化后不变。只有 Model 矩阵每帧更新。可以预先计算 Projection × View。

---

## 下一步

Day 10 将学习：
- 纹理基础
- UV 坐标映射
- 给矩形贴图