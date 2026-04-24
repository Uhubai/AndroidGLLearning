# Day 10: 纹理基础

## 学习目标

- 理解纹理和纹理坐标（UV 坐标）
- 学习纹理加载和绑定流程
- 掌握纹理坐标翻转问题
- 给矩形贴图

---

## 核心概念

### 1. 什么是纹理？

```
┌─────────────────────────────────────────────────────────────────┐
│                    纹理映射概念                                   │
└─────────────────────────────────────────────────────────────────┘

几何图形（矩形）         纹理（图片）           纹理映射后
┌──────────┐            ┌──────────┐          ┌──────────┐
│          │            │  🖼️     │          │          │
│          │     +      │  图片    │    =     │  带贴图   │
│          │            │          │          │  的矩形   │
└──────────┘            └──────────┘          └──────────┘
(只有形状)              (只有颜色)            (形状+颜色)
```

### 2. UV 坐标系统

```
┌─────────────────────────────────────────────────────────────────┐
│                    UV 坐标系统                                   │
└─────────────────────────────────────────────────────────────────┘

纹理坐标系：
- U 轴（水平）：0 → 1（从左到右）
- V 轴（垂直）：0 → 1（从下到上）⚠️ 注意：与图片坐标系相反！

(0, 1) ─────────── (1, 1)
  │                  │
  │      纹理        │
  │                  │
(0, 0) ─────────── (1, 0)

Android 图片坐标系（与 UV 不同）：
(0, 0) ─────────── (width, 0)
  │                  │
  │      图片        │
  │                  │
(0, height) ───── (width, height)

关键区别：
- UV 坐标：原点在左下角
- 图片坐标：原点在左上角
- 因此需要翻转 V 坐标！
```

### 3. 纹理加载流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    纹理加载完整流程                               │
└─────────────────────────────────────────────────────────────────┘

1. 生成纹理 ID
   │
   ▼
2. 绑定纹理到目标
   │
   ▼
3. 设置纹理参数（环绕、过滤）
   │
   ▼
4. 加载图片数据到 GPU
   │
   ▼
5. 生成 Mipmap（可选）
   │
   ▼
6. 在着色器中使用
```

---

## 实现步骤

### 1. 顶点数据结构变化

```kotlin
// 之前：位置 + 颜色
private val DATA = floatArrayOf(
    -50f,  50f,  1.0f, 0.0f, 0.0f,  // 位置 + 颜色
    -50f, -50f,  0.0f, 1.0f, 0.0f,
     50f,  50f,  0.0f, 0.0f, 1.0f,
     50f, -50f,  1.0f, 1.0f, 0.0f
)

// 现在：位置 + UV 坐标
private val DATA = floatArrayOf(
    -50f,  50f,  0.0f, 1.0f,  // 位置 + UV
    -50f, -50f,  0.0f, 0.0f,
     50f,  50f,  1.0f, 1.0f,
     50f, -50f,  1.0f, 0.0f
)
```

### 2. 顶点着色器变化

```glsl
// 之前
attribute vec4 a_Color;        // 接收顶点颜色
varying vec4 v_Color;          // 传递给片段着色器

// 现在
attribute vec2 a_TextureCoord; // 接收 UV 坐标
varying vec2 v_TextureCoord;   // 传递给片段着色器
```

### 3. 片段着色器变化

```glsl
precision mediump float;
varying vec2 v_TextureCoord;
uniform sampler2D u_Texture;   // 纹理采样器

void main() {
    gl_FragColor = texture2D(u_Texture, v_TextureCoord);
}
```

### 4. 纹理加载代码

```kotlin
// 生成纹理 ID
val textureId = IntArray(1)
GLES20.glGenTextures(1, textureId, 0)

// 绑定纹理
GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0])

// 设置纹理参数
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

// 加载图片
val bitmap = BitmapFactory.decodeResource(resources, R.drawable.sample)
GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
bitmap.recycle()
```

---

## 纹理参数说明

| 参数 | 值 | 说明 |
|------|-----|------|
| `GL_TEXTURE_MIN_FILTER` | `GL_LINEAR` | 缩小时的过滤方式 |
| `GL_TEXTURE_MIN_FILTER` | `GL_NEAREST` | 像素化效果 |
| `GL_TEXTURE_MAG_FILTER` | `GL_LINEAR` | 放大时的过滤方式 |
| `GL_TEXTURE_WRAP_S` | `GL_REPEAT` | 水平方向重复 |
| `GL_TEXTURE_WRAP_T` | `GL_REPEAT` | 垂直方向重复 |

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **纹理** | 2D 图片数据，用于映射到几何图形 |
| **UV 坐标** | 纹理坐标系统，范围 [0, 1] |
| **sampler2D** | 着色器中的纹理采样器类型 |
| **texture2D** | 片段着色器中采样纹理的函数 |
| **GL_LINEAR** | 线性过滤，平滑效果 |
| **GL_NEAREST** | 最近邻过滤，像素化效果 |

---

## 常见问题

**Q: UV 坐标为什么 V 轴从下到上？**
A: OpenGL 的纹理坐标系统约定，与图片坐标系（从上到下）不同，需要翻转。

**Q: GL_LINEAR 和 GL_NEAREST 的区别？**
A: GL_LINEAR 平滑插值，适合照片；GL_NEAREST 保持像素，适合像素艺术。

**Q: 纹理大小必须是 2 的幂吗？**
A: OpenGL ES 2.0 推荐 2 的幂（32, 64, 128...），但不是强制要求。

---

## 下一步

Day 11 将学习：
- 纹理变换
- 纹理重复
- 多纹理混合