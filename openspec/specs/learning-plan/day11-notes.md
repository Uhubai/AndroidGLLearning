# Day 11: 纹理变换

## 学习目标

- 理解纹理矩阵（UV 变换）
- 实现纹理旋转、缩放、平移
- 掌握纹理重复模式（GL_REPEAT）
- 学习多纹理混合技术

---

## 核心概念

### 1. 纹理变换原理

```
┌─────────────────────────────────────────────────────────────────┐
│                    纹理变换概念                                   │
└─────────────────────────────────────────────────────────────────┘

顶点变换：移动、旋转、缩放几何图形
纹理变换：移动、旋转、缩放纹理本身

示例：
- 纹理平移：水流、云层移动效果
- 纹理旋转：漩涡、风扇效果
- 纹理缩放：放大/缩小纹理细节
```

### 2. UV 变换矩阵

```
纹理变换矩阵（2D）：
┌                         ┐
│ cos(θ)  -sin(θ)  tx     │
│ sin(θ)   cos(θ)  ty     │
│ 0        0       1      │
└                         ┘

应用方式：
- 在顶点着色器中变换 UV 坐标
- 或在片段着色器中变换采样坐标
```

### 3. 纹理环绕模式

```
┌─────────────────────────────────────────────────────────────────┐
│                    纹理环绕模式                                   │
└─────────────────────────────────────────────────────────────────┘

GL_REPEAT：         GL_CLAMP_TO_EDGE：    GL_MIRRORED_REPEAT：
┌───┬───┬───┐      ┌───┐                  ┌───┬───┬───┐
│ B │ A │ B │      │A│                    │ B │ A │ B │
├───┼───┼───┤      ├───┤                  ├───┼───┼───┤
│ A │ A │ A │      │A│                    │ A │ A │ A │
└───┴───┴───┘      └───┘                  └───┴───┴───┘
（重复）             （边缘延伸）             （镜像重复）
```

---

## 实现步骤

### 1. 顶点着色器（添加 UV 变换）

```glsl
uniform mat4 u_Matrix;        // MVP 矩阵
uniform mat4 u_TextureMatrix; // 纹理变换矩阵

attribute vec4 a_Position;
attribute vec2 a_TextureCoord;
varying vec2 v_TextureCoord;

void main() {
    gl_Position = u_Matrix * a_Position;
    // 应用纹理变换
    v_TextureCoord = (u_TextureMatrix * vec4(a_TextureCoord, 0.0, 1.0)).xy;
}
```

### 2. 创建纹理变换矩阵

```kotlin
private val textureMatrix = FloatArray(16)

// 纹理旋转
Matrix.setIdentityM(textureMatrix, 0)
Matrix.rotateM(textureMatrix, 0, angle, 0f, 0f, 1f)

// 纹理平移
Matrix.translateM(textureMatrix, 0, tu, tv, 0f)

// 纹理缩放
Matrix.scaleM(textureMatrix, 0, su, sv, 1f)
```

### 3. 纹理重复模式

```kotlin
// 设置重复模式
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

// UV 坐标超过 1.0 时会自动重复
val uv = floatArrayOf(
    0.0f, 2.0f,  // 重复 2 次
    0.0f, 0.0f,
    2.0f, 2.0f,
    2.0f, 0.0f
)
```

---

## 多纹理混合

### 原理

```
纹理1（底色）  +  纹理2（图案）  =  混合效果
     │                │                │
     ▼                ▼                ▼
   砖墙纹理      +    污渍纹理    =   老旧砖墙
```

### 着色器实现

```glsl
uniform sampler2D u_Texture1;
uniform sampler2D u_Texture2;
uniform float u_MixFactor;  // 混合系数 0.0 ~ 1.0

void main() {
    vec4 color1 = texture2D(u_Texture1, v_TextureCoord);
    vec4 color2 = texture2D(u_Texture2, v_TextureCoord);
    gl_FragColor = mix(color1, color2, u_MixFactor);
}
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **纹理矩阵** | 用于变换 UV 坐标的矩阵 |
| **GL_REPEAT** | 纹理重复模式 |
| **GL_CLAMP_TO_EDGE** | 边缘延伸模式 |
| **mix()** | GLSL 混合函数 |
| **UV 动画** | 通过变换 UV 坐标创建动画效果 |

---

## 下一步

Day 12 将学习：
- 相机纹理（OES 外部纹理）
- SurfaceTexture 使用
- 相机预览到 GLSurfaceView