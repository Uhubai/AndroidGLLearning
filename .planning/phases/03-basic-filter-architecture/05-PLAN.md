---
wave: 3
depends_on: [02-PLAN.md, 03-PLAN.md, 04-PLAN.md]
objective: 实现 Day19Renderer - 滤镜架构设计（可切换滤镜管理器）
requirements_addressed: [BASIC_FILTER-04]
files_modified:
  - app/src/main/java/com/example/glearning/Day19Renderer.kt
  - app/src/main/java/com/example/glearning/FilterManager.kt
  - app/src/main/java/com/example/glearning/MainActivity.kt
autonomous: true
---

# Plan 05: Day19Renderer - 滤镜架构设计

## Context

Day 19 基于 Day16-18 的实践，设计可切换的滤镜管理器架构。这是 Phase 3 的核心架构设计日。

**关键决策**:
- D-05 to D-08: 基类设计已包含完整渲染管线
- D-19: 滤镜架构设计（可切换滤镜管理器）

## Tasks

### Task 1: 创建 FilterManager 类

<read_first>
- app/src/main/java/com/example/glearning/Day16Renderer.kt
- app/src/main/java/com/example/glearning/Day17Renderer.kt
- app/src/main/java/com/example/glearning/Day18Renderer.kt
- AGENTS.md
</read_first>

<acceptance_criteria>
- FilterManager.kt 文件创建
- 包含 Filter 接口定义（getFragmentShader、setupUniforms、onDrawFrame）
- 包含 FilterManager 类管理滤镜列表和当前滤镜
- 支持添加、切换、获取当前滤镜
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 FilterManager 架构：

```kotlin
interface Filter {
    fun getFragmentShader(): String
    fun setupUniforms(program: Int)
    fun onDrawFrame(renderer: BaseFilterRenderer)
}

class FilterManager {
    private val filters = mutableListOf<Filter>()
    private var currentFilterIndex = -1
    
    fun addFilter(filter: Filter) {
        filters.add(filter)
        if (currentFilterIndex == -1) currentFilterIndex = 0
    }
    
    fun getCurrentFilter(): Filter? = 
        if (currentFilterIndex >= 0) filters[currentFilterIndex] else null
    
    fun switchToNext() {
        if (filters.isNotEmpty()) {
            currentFilterIndex = (currentFilterIndex + 1) % filters.size
        }
    }
    
    fun switchToPrevious() {
        if (filters.isNotEmpty()) {
            currentFilterIndex = (currentFilterIndex - 1 + filters.size) % filters.size
        }
    }
}
```
</action>

### Task 2: 创建 Day19Renderer 实现滤镜切换

<read_first>
- .planning/phases/03-basic-filter-architecture/01-PLAN.md
- app/src/main/java/com/example/glearning/Day16Renderer.kt
</read_first>

<acceptance_criteria>
- Day19Renderer.kt 文件创建
- 类继承 BaseFilterRenderer
- 使用 FilterManager 管理 Day16-18 的滤镜
- 点击切换滤镜
- 显示当前滤镜名称（使用 OpenGL 绘制文本或简单指示）
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 Day19Renderer：

1. 初始化 FilterManager 并添加 Day16-18 的滤镜实例
2. 在 getFragmentShader() 中返回当前滤镜的片段着色器
3. 在 setupUniforms() 中调用当前滤镜的 setupUniforms
4. 实现触摸切换：点击屏幕调用 filterManager.switchToNext()
5. 显示当前滤镜名称（可选：使用简单矩形和颜色指示）
</action>

### Task 3: 集成到 MainActivity

<read_first>
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- MainActivity.kt 的 createRenderer() when 表达式添加 Day19 分支
- showRendererSelector() 添加 Day19 菜单项
</acceptance_criteria>

<action>
修改 MainActivity.kt：

1. 在 createRenderer(day) when 表达式中添加：
   ```kotlin
   19 -> Day19Renderer()
   ```

2. 在 showRendererSelector() 菜单中添加：
   ```kotlin
   menu.add(0, 19, 19, "Day 19: 滤镜管理器")
   ```
</action>

---

*Plan 05 created: 2026-05-11*
