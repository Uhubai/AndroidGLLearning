---
wave: 2
depends_on: [01-PLAN.md]
objective: 实现 Day18Renderer - 色调/色温调节
requirements_addressed: [BASIC_FILTER-03]
files_modified:
  - app/src/main/java/com/example/glearning/Day18Renderer.kt
  - app/src/main/java/com/example/glearning/MainActivity.kt
autonomous: true
---

# Plan 04: Day18Renderer - 色调/色温调节

## Context

Day 18 实现色调和色温调节滤镜，涉及 RGB↔HSL/HSV 颜色空间转换。继承 BaseFilterRenderer。

**关键决策**:
- D-07: 子类只需提供片段着色器和 uniform 参数
- D-09: 参数变化时才更新 uniform

## Tasks

### Task 1: 创建 Day18Renderer 类

<read_first>
- .planning/phases/03-basic-filter-architecture/01-PLAN.md
- app/src/main/java/com/example/glearning/Day16Renderer.kt
- AGENTS.md
</read_first>

<acceptance_criteria>
- Day18Renderer.kt 文件创建
- 类继承 BaseFilterRenderer
- 实现 getFragmentShader() 包含 RGB↔HSV 转换函数
- 包含 hueShift（色调偏移）和 temperature（色温）uniform
- 实现 setupUniforms() 设置 uniform 句柄
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 Day18Renderer 类：

片段着色器包含：
1. RGB→HSV 转换函数
2. HSV→RGB 转换函数
3. 色调偏移：修改 H 分量
4. 色温调节：修改 B 分量（暖色调增加红色，冷色调增加蓝色）

```glsl
// RGB to HSV
vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

// HSV to RGB
vec3 hsv2rgb(vec3 c) {
    vec3 rgb = clamp(abs(mod(c.x * 6.0 + vec3(0.0, 4.0, 2.0), 6.0) - 3.0) - 1.0, 0.0, 1.0);
    return c.z * mix(vec3(1.0), rgb, c.y);
}
```

uniform 参数：
- u_HueShift: float（-180° ~ 180°，归一化到 -1.0 ~ 1.0）
- u_Temperature: float（冷 -1.0 ~ 暖 1.0）
</action>

### Task 2: 集成到 MainActivity

<read_first>
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- MainActivity.kt 的 createRenderer() when 表达式添加 Day18 分支
- showRendererSelector() 添加 Day18 菜单项
</acceptance_criteria>

<action>
修改 MainActivity.kt：

1. 在 createRenderer(day) when 表达式中添加：
   ```kotlin
   18 -> Day18Renderer()
   ```

2. 在 showRendererSelector() 菜单中添加：
   ```kotlin
   menu.add(0, 18, 18, "Day 18: 色调/色温")
   ```
</action>

---

*Plan 04 created: 2026-05-11*
