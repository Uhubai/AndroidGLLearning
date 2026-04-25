# Day 13: 复习 - 多物体变换与纹理综合练习

## 学习目标

- 复习 MVP 矩阵组合流程
- 复习纹理加载和绑定
- 实现多物体不同动画效果

---

## 复习内容

### 1. MVP 矩阵综合

```kotlin
// 完整的 MVP 组合流程
Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, centerX, centerY, centerZ, upX, upY, upZ)
Matrix.orthoM(projectionMatrix, 0, left, right, bottom, top, near, far)

// 每帧计算
Matrix.setIdentityM(modelMatrix, 0)
Matrix.translateM(modelMatrix, 0, tx, ty, tz)  // 平移
Matrix.rotateM(modelMatrix, 0, angle, 0f, 0f, 1f)  // 旋转
Matrix.scaleM(modelMatrix, 0, sx, sy, sz)  // 缩放

Matrix.multiplyMM(tempMatrix, 0, viewMatrix, 0, modelMatrix, 0)
Matrix.multiplyMM(resultMatrix, 0, projectionMatrix, 0, tempMatrix, 0)
```

### 2. 纹理综合

```kotlin
// 纹理创建流程
GLES20.glGenTextures(1, textureId, 0)
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
```

### 3. 动画综合

```kotlin
// 基于时间的动画
val elapsedSeconds = (currentTime - startTime) / 1000f

// 平移动画
val offsetY = sin(elapsedSeconds * 2f) * 20f

// 旋转动画
val angle = elapsedSeconds * 60f

// 缩放动画
val scale = 1.0f + sin(elapsedSeconds * 2f) * 0.3f
```

---

## 实现效果

6 个矩形圆形排列，每个有不同的变换和纹理：
- 矩形 0,3：平移动画 + 条纹纹理
- 矩形 1,4：旋转动画 + 漩涡纹理
- 矩形 2,5：缩放动画 + 渐变纹理

---

## 下一步

Day 14: 更复杂的综合动画（纹理变换 + 多纹理混合）