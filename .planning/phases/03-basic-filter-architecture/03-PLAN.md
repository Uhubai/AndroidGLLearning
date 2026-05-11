---
wave: 2
depends_on: [01-PLAN.md]
objective: 实现 Day17Renderer - 灰度/反色/Sepia 经典滤镜
requirements_addressed: [BASIC_FILTER-02]
files_modified:
  - app/src/main/java/com/example/glearning/Day17Renderer.kt
  - app/src/main/java/com/example/glearning/MainActivity.kt
autonomous: true
---

# Plan 03: Day17Renderer - 灰度/反色/Sepia 经典滤镜

## Context

Day 17 实现三种经典滤镜：灰度、反色、Sepia。继承 BaseFilterRenderer，通过 uniform 参数切换滤镜类型。

**关键决策**:
- D-07: 子类只需提供片段着色器和 uniform 参数
- D-09: 参数变化时才更新 uniform
- D-16 to D-20: 代码规范

## Tasks

### Task 1: 创建 Day17Renderer 类

<read_first>
- .planning/phases/03-basic-filter-architecture/01-PLAN.md
- app/src/main/java/com/example/glearning/Day16Renderer.kt
- AGENTS.md
</read_first>

<acceptance_criteria>
- Day17Renderer.kt 文件创建
- 类继承 BaseFilterRenderer
- 实现 getFragmentShader() 返回包含三种滤镜的片段着色器
- 包含 filterType uniform（0=原图，1=灰度，2=反色，3=Sepia）
- 实现 setupUniforms() 设置 filterType uniform
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 Day17Renderer 类：

```kotlin
class Day17Renderer : BaseFilterRenderer() {
    companion object {
        private const val TAG = "Day17Renderer"
        private const val WORLD_HALF_SIZE = 100f
    }
    
    // 滤镜类型：0=原图，1=灰度，2=反色，3=Sepia
    private var filterType = 0
    private var lastFilterType = -1
    
    private var uFilterType = 0
    
    override fun getFragmentShader(): String = """
        precision mediump float;
        varying vec2 v_TextureCoord;
        uniform sampler2D u_Texture;
        uniform int u_FilterType;
        
        void main() {
            vec4 color = texture2D(u_Texture, v_TextureCoord);
            
            if (u_FilterType == 1) {
                // 灰度
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                color.rgb = vec3(gray);
            } else if (u_FilterType == 2) {
                // 反色
                color.rgb = 1.0 - color.rgb;
            } else if (u_FilterType == 3) {
                // Sepia
                float r = color.r * 0.393 + color.g * 0.769 + color.b * 0.189;
                float g = color.r * 0.349 + color.g * 0.686 + color.b * 0.168;
                float b = color.r * 0.272 + color.g * 0.534 + color.b * 0.131;
                color.rgb = vec3(r, g, b);
            }
            
            gl_FragColor = color;
        }
    """
    
    override fun setupUniforms(program: Int) {
        uFilterType = GLES20.glGetUniformLocation(program, "u_FilterType")
    }
}
```
</action>

### Task 2: 实现滤镜切换方法

<read_first>
- app/src/main/java/com/example/glearning/Day16Renderer.kt
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- 包含 setFilterType(type: Int) 方法
- 使用 GLSurfaceView.queueEvent() 安全切换
- 支持 0-3 四种模式
</acceptance_criteria>

<action>
添加滤镜切换方法：

```kotlin
fun setFilterType(type: Int) {
    filterType = type.coerceIn(0, 3)
}
```

在 onDrawFrame 中：
```kotlin
if (filterType != lastFilterType) {
    GLES20.glUniform1i(uFilterType, filterType)
    lastFilterType = filterType
}
```

在 MainActivity 中提供切换接口（可通过菜单或触摸）
</action>

### Task 3: 集成到 MainActivity

<read_first>
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- MainActivity.kt 的 createRenderer() when 表达式添加 Day17 分支
- showRendererSelector() 添加 Day17 菜单项
</acceptance_criteria>

<action>
修改 MainActivity.kt：

1. 在 createRenderer(day) when 表达式中添加：
   ```kotlin
   17 -> Day17Renderer()
   ```

2. 在 showRendererSelector() 菜单中添加：
   ```kotlin
   menu.add(0, 17, 17, "Day 17: 灰度/反色/Sepia")
   ```
</action>

---

*Plan 03 created: 2026-05-11*
