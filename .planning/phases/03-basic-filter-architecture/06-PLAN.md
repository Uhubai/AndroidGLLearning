---
wave: 4
depends_on: [05-PLAN.md]
objective: 实现 Day20Renderer - 纯 OpenGL UI 控件（按钮、滑块）
requirements_addressed: [BASIC_FILTER-05]
files_modified:
  - app/src/main/java/com/example/glearning/Day20Renderer.kt
  - app/src/main/java/com/example/glearning/OpenGLUI.kt
  - app/src/main/java/com/example/glearning/MainActivity.kt
autonomous: true
---

# Plan 06: Day20Renderer - 纯 OpenGL UI 控件

## Context

Day 20 实现纯 OpenGL 绘制的 UI 控件（按钮、滑块），用于滤镜参数调节。这是 Phase 3 的 UI 部分。

**关键决策**:
- D-10: 纯 OpenGL 绘制 UI
- D-11: 完整控件库（按钮、滑块等）
- D-12: 底部面板布局
- D-13: Day 20 实现核心控件（按钮、滑块）
- D-14: UI 与滤镜内容分层渲染
- D-15: 触摸事件处理

## Tasks

### Task 1: 创建 OpenGLUI 辅助类

<read_first>
- app/src/main/java/com/example/glearning/Day4Renderer.kt
- app/src/main/java/com/example/glearning/Day14Renderer.kt
- AGENTS.md
</read_first>

<acceptance_criteria>
- OpenGLUI.kt 文件创建
- 包含 OpenGLButton 类（矩形 + 文本区域）
- 包含 OpenGLSlider 类（轨道 + 滑块）
- 使用正交投影绘制 2D UI
- 包含触摸检测方法
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 OpenGLUI 辅助类：

```kotlin
class OpenGLUI {
    companion object {
        // UI 正交投影（屏幕坐标）
        fun createUIOrthoMatrix(width: Int, height: Int): FloatArray {
            return FloatArray(16).apply {
                Matrix.orthoM(this, 0, 0f, width.toFloat(), 0f, height.toFloat(), -1f, 1f)
            }
        }
    }
}

class OpenGLButton(
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    val label: String,
    val color: FloatArray = floatArrayOf(0.3f, 0.6f, 0.9f, 1.0f)
) {
    fun draw() { /* 绘制矩形按钮 */ }
    fun containsPoint(px: Float, py: Float): Boolean {
        return px in x..(x + width) && py in y..(y + height)
    }
}

class OpenGLSlider(
    val x: Float, val y: Float,
    val width: Float, val height: Float,
    var value: Float = 0.5f,
    val min: Float = 0f, val max: Float = 1f
) {
    fun draw() { /* 绘制轨道和滑块 */ }
    fun containsPoint(px: Float, py: Float): Boolean { ... }
    fun updateValue(px: Float) {
        value = ((px - x) / width).coerceIn(0f, 1f)
        value = min + value * (max - min)
    }
}
```
</action>

### Task 2: 创建 Day20Renderer 实现底部面板 UI

<read_first>
- .planning/phases/03-basic-filter-architecture/01-PLAN.md
- app/src/main/java/com/example/glearning/Day16Renderer.kt
- AGENTS.md
</read_first>

<acceptance_criteria>
- Day20Renderer.kt 文件创建
- 类继承 BaseFilterRenderer
- 实现底部面板（约占屏幕 20-30% 高度）
- 绘制按钮（滤镜切换）和滑块（参数调节）
- 实现触摸事件处理（检测点击/拖拽）
- UI 与滤镜内容分层渲染（先绘制滤镜，再绘制 UI）
- 包含类级 KDoc 注释
</acceptance_criteria>

<action>
创建 Day20Renderer：

1. 继承 BaseFilterRenderer，复用滤镜渲染逻辑
2. 在 onSurfaceChanged 中创建 UI 正交投影矩阵
3. 在 onDrawFrame 中：
   - 先绘制滤镜内容（使用 MVP 投影）
   - 切换到 UI 正交投影
   - 绘制底部面板（半透明矩形）
   - 绘制按钮和滑块
4. 实现触摸处理：
   - 检测触摸点是否在按钮/滑块上
   - 按钮点击：切换滤镜
   - 滑块拖拽：更新参数值
5. 底部面板高度约为屏幕高度的 25%
</action>

### Task 3: 集成到 MainActivity

<read_first>
- app/src/main/java/com/example/glearning/MainActivity.kt
</read_first>

<acceptance_criteria>
- MainActivity.kt 的 createRenderer() when 表达式添加 Day20 分支
- showRendererSelector() 添加 Day20 菜单项
- 正确传递触摸事件到 Renderer
</acceptance_criteria>

<action>
修改 MainActivity.kt：

1. 在 createRenderer(day) when 表达式中添加：
   ```kotlin
   20 -> Day20Renderer()
   ```

2. 在 showRendererSelector() 菜单中添加：
   ```kotlin
   menu.add(0, 20, 20, "Day 20: OpenGL UI 控件")
   ```

3. 确保触摸事件正确传递到 Day20Renderer
</action>

---

*Plan 06 created: 2026-05-11*
