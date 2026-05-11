---
wave: 2
depends_on: [01-PLAN.md]
objective: 实现 Day16Renderer - 亮度/对比度/饱和度可调滤镜
requirements_addressed: [BASIC_FILTER-01]
files_modified:
  - app/src/main/java/com/example/glearning/Day16Renderer.kt
  - app/src/main/java/com/example/glearning/MainActivity.kt
autonomous: true
---

# Plan 02: Day16Renderer - 亮度/对比度/饱和度滤镜

## Context

Day 16 实现第一个基础滤镜：亮度、对比度、饱和度可调节。继承 BaseFilterRenderer，只需实现片段着色器和参数设置。

**关键决策**:
- D-01 to D-04: 双模式策略，默认程序化纹理，点击切换相机
- D-07: 子类只需提供片段着色器和 uniform 参数
- D-09: 参数变化时才更新 uniform
- D-16 to D-20: 代码规范

## Tasks

### Task 1: 创建 Day16Renderer 类

<read_first>
- .planning/phases/03-basic-filter-architecture/01-PLAN.md
- app/src/main/java/com/example/glearning/Day15Renderer.kt
- app/src/main/java/com/example/glearning/Day14Renderer.kt
- AGENTS.md
</read_first>

<acceptance_criteria>
- Day16Renderer.kt 文件创建
- 类继承 BaseFilterRenderer
- 实现 getFragmentShader() 返回亮度/对比度/饱和度片段着色器
- 实现 setupUniforms() 设置 brightness、contrast、saturation uniform
- 包含 companion object 中的 WORLD_HALF_SIZE = 100f
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 Day16Renderer 类：

```kotlin
class Day16Renderer : BaseFilterRenderer() {
    companion object {
        private const val TAG = "Day16Renderer"
        private const val WORLD_HALF_SIZE = 100f
    }
    
    // 滤镜参数
    private var brightness = 0f      // 范围 -1.0 ~ 1.0
    private var contrast = 1.0f      // 范围 0.0 ~ 2.0
    private var saturation = 1.0f    // 范围 0.0 ~ 2.0
    
    // 缓存上次值
    private var lastBrightness = Float.MAX_VALUE
    private var lastContrast = Float.MAX_VALUE
    private var lastSaturation = Float.MAX_VALUE
    
    // uniform 句柄
    private var uBrightness = 0
    private var uContrast = 0
    private var uSaturation = 0
    
    override fun getFragmentShader(): String = """..."""
    
    override fun setupUniforms(program: Int) {
        uBrightness = GLES20.glGetUniformLocation(program, "u_Brightness")
        uContrast = GLES20.glGetUniformLocation(program, "u_Contrast")
        uSaturation = GLES20.glGetUniformLocation(program, "u_Saturation")
    }
}
```

片段着色器实现：
- 亮度：color.rgb += brightness
- 对比度：color.rgb = (color.rgb - 0.5) * contrast + 0.5
- 饱和度：使用灰度混合，gray = dot(color.rgb, vec3(0.299, 0.587, 0.114))，mix(gray, color.rgb, saturation)
</action>

### Task 2: 实现触摸切换逻辑

<read_first>
- app/src/main/java/com/example/glearning/Day15Renderer.kt
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- 实现 onTouchEvent 或相关触摸事件处理
- 点击屏幕切换 useCameraTexture 标志
- 切换时重新创建纹理
- 使用 GLSurfaceView.queueEvent() 安全传递到渲染线程
</acceptance_criteria>

<action>
添加触摸切换方法：

```kotlin
fun toggleTextureMode() {
    useCameraTexture = !useCameraTexture
    // 需要重新初始化纹理
}
```

在 MainActivity 中：
- 为 Day16Renderer 添加触摸事件监听
- 使用 glSurfaceView.queueEvent { renderer.toggleTextureMode() }
- 调用 glSurfaceView.requestRender() 触发重绘
</action>

### Task 3: 集成到 MainActivity

<read_first>
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- MainActivity.kt 的 createRenderer() when 表达式添加 Day16 分支
- showRendererSelector() 添加 Day16 菜单项
- 正确设置 RENDERMODE_CONTINUOUSLY
</acceptance_criteria>

<action>
修改 MainActivity.kt：

1. 在 createRenderer(day) when 表达式中添加：
   ```kotlin
   16 -> Day16Renderer()
   ```

2. 在 showRendererSelector() 菜单中添加：
   ```kotlin
   menu.add(0, 16, 16, "Day 16: 亮度/对比度/饱和度")
   ```

3. 确保 Day16 使用 RENDERMODE_CONTINUOUSLY（支持动画）
</action>

---

*Plan 02 created: 2026-05-11*
