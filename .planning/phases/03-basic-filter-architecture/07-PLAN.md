---
wave: 4
depends_on: [06-PLAN.md]
objective: 实现 Day21Renderer - 扩展 UI 控件（单选框、复选框、下拉菜单）
requirements_addressed: [BASIC_FILTER-05]
files_modified:
  - app/src/main/java/com/example/glearning/Day21Renderer.kt
  - app/src/main/java/com/example/glearning/OpenGLUI.kt
  - app/src/main/java/com/example/glearning/MainActivity.kt
autonomous: true
---

# Plan 07: Day21Renderer - 扩展 UI 控件

## Context

Day 21 扩展 Day20 的 UI 控件库，添加单选框、复选框、下拉菜单等完整控件。

**关键决策**:
- D-11: 完整控件库
- D-12: 底部面板布局
- D-13: Day 21 扩展其他控件

## Tasks

### Task 1: 扩展 OpenGLUI 类添加新控件

<read_first>
- app/src/main/java/com/example/glearning/OpenGLUI.kt
- app/src/main/java/com/example/glearning/Day20Renderer.kt
</read_first>

<acceptance_criteria>
- OpenGLUI.kt 添加 OpenGLCheckBox 类
- OpenGLUI.kt 添加 OpenGLRadioButton 类
- OpenGLUI.kt 添加 OpenGLDropdown 类
- 所有新控件包含 draw() 和 containsPoint() 方法
- 支持触摸交互
</acceptance_criteria>

<action>
在 OpenGLUI.kt 中添加：

```kotlin
class OpenGLCheckBox(
    val x: Float, val y: Float,
    val label: String,
    var isChecked: Boolean = false
) {
    fun draw() { /* 绘制复选框 + 文本 */ }
    fun toggle() { isChecked = !isChecked }
    fun containsPoint(px: Float, py: Float): Boolean { ... }
}

class OpenGLRadioButton(
    val x: Float, val y: Float,
    val label: String,
    var isSelected: Boolean = false
) {
    fun draw() { /* 绘制圆形单选框 + 文本 */ }
    fun select() { isSelected = true }
    fun containsPoint(px: Float, py: Float): Boolean { ... }
}

class OpenGLDropdown(
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val options: List<String>,
    var selectedIndex: Int = 0
) {
    var isExpanded = false
    fun draw() { /* 绘制下拉菜单 */ }
    fun containsPoint(px: Float, py: Float): Boolean { ... }
    fun handleTouch(px: Float, py: Float): Boolean { ... }
}
```
</action>

### Task 2: 创建 Day21Renderer 实现完整控件面板

<read_first>
- app/src/main/java/com/example/glearning/Day20Renderer.kt
- AGENTS.md
</readance_criteria>

<acceptance_criteria>
- Day21Renderer.kt 文件创建
- 类继承 BaseFilterRenderer
- 实现包含所有控件类型的底部面板
- 展示控件库的完整功能
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 Day21Renderer：

1. 继承 BaseFilterRenderer
2. 在底部面板中展示所有控件类型：
   - 按钮：滤镜切换
   - 滑块：亮度/对比度/饱和度调节
   - 复选框：启用/禁用某些效果
   - 单选框：选择滤镜模式
   - 下拉菜单：选择预设参数
3. 实现完整的触摸交互
4. 展示控件库的实际应用效果
</action>

### Task 3: 集成到 MainActivity

<read_first>
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- MainActivity.kt 的 createRenderer() when 表达式添加 Day21 分支
- showRendererSelector() 添加 Day21 菜单项
</acceptance_criteria>

<action>
修改 MainActivity.kt：

1. 在 createRenderer(day) when 表达式中添加：
   ```kotlin
   21 -> Day21Renderer()
   ```

2. 在 showRendererSelector() 菜单中添加：
   ```kotlin
   menu.add(0, 21, 21, "Day 21: 完整控件库")
   ```
</action>

---

*Plan 07 created: 2026-05-11*
