# Day 12: 纹理混合 - 多纹理叠加

## 学习目标

- 理解多纹理混合原理
- 掌握 GLSL mix() 函数
- 实现纹理叠加效果
- 学习混合模式（正片叠底、滤色、叠加）

---

## 核心概念

### 1. 纹理混合原理

```
┌─────────────────────────────────────────────────────────────────┐
│                    纹理混合概念                                   │
└─────────────────────────────────────────────────────────────────┘

纹理1（底色）  +  纹理2（图案）  =  混合效果
     │                │                │
     ▼                ▼                ▼
   砖墙纹理      +    污渍纹理    =   老旧砖墙
   
混合方式：
- 线性混合：color = mix(tex1, tex2, factor)
- 正片叠底：color = tex1 * tex2
- 滤色：color = 1 - (1-tex1) * (1-tex2)
- 叠加：根据底色决定混合方式
```

### 2. GLSL 混合函数

```glsl
// 线性混合
mix(color1, color2, factor)  // factor: 0.0 ~ 1.0

// 正片叠底（变暗）
color1 * color2

// 滤色（变亮）
1.0 - (1.0 - color1) * (1.0 - color2)

// 叠加
color1 < 0.5 ? 2.0 * color1 * color2 : 1.0 - 2.0 * (1.0 - color1) * (1.0 - color2)
```

---

## 实现步骤

### 1. 顶点着色器（支持多纹理坐标）

```glsl
uniform mat4 u_Matrix;
attribute vec4 a_Position;
attribute vec2 a_TextureCoord;
varying vec2 v_TextureCoord;

void main() {
    gl_Position = u_Matrix * a_Position;
    v_TextureCoord = a_TextureCoord;
}
```

### 2. 片段着色器（多纹理混合）

```glsl
precision mediump float;
varying vec2 v_TextureCoord;
uniform sampler2D u_Texture1;
uniform sampler2D u_Texture2;
uniform float u_MixFactor;
uniform int u_BlendMode;  // 0=线性，1=正片叠底，2=滤色，3=叠加

void main() {
    vec4 color1 = texture2D(u_Texture1, v_TextureCoord);
    vec4 color2 = texture2D(u_Texture2, v_TextureCoord);
    
    vec4 result;
    if (u_BlendMode == 0) {
        result = mix(color1, color2, u_MixFactor);
    } else if (u_BlendMode == 1) {
        result = color1 * color2;
    } else if (u_BlendMode == 2) {
        result = 1.0 - (1.0 - color1) * (1.0 - color2);
    } else {
        result = mix(color1, color2, u_MixFactor);
    }
    
    gl_FragColor = result;
}
```

---

## 关键术语

| 术语 | 解释 |
|------|------|
| **mix()** | GLSL 线性插值函数 |
| **正片叠底** | 相乘混合，结果变暗 |
| **滤色** | 反相相乘，结果变亮 |
| **叠加** | 根据底色选择混合方式 |
| **多纹理** | 同时绑定多个纹理单元 |

---

## 下一步

Day 13-14：复习
- 纹理 + 变换综合动画
- 准备进入第 3 周：相机纹理