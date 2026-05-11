# Phase 3: 基础滤镜架构 - Context

**Gathered:** 2026-05-11
**Status:** Ready for planning

## Phase Boundary

本阶段实现可调节的基础滤镜和滤镜管理架构。包含：
- Day 16: 亮度/对比度/饱和度可调滤镜
- Day 17: 灰度/反色/Sepia 经典滤镜
- Day 18: 色调/色温调节
- Day 19: 滤镜架构设计（可切换滤镜管理器）
- Day 20-21: 纯 OpenGL 绘制的滤镜面板 UI + 参数调节

**不包含**: 卷积滤镜、FBO、LUT、美颜、贴纸等高级特效（属于 Phase 4）

## Implementation Decisions

### 滤镜输入源策略
- **D-01:** Day 16-18 采用双模式策略 — 同时支持程序化纹理和相机预览
- **D-02:** 切换方式：屏幕触摸切换（点击屏幕在两种模式间切换）
- **D-03:** 默认显示程序化纹理（用于学习滤镜算法），点击切换到相机预览（验证实战效果）
- **D-04:** 使用 Day14 的程序化纹理作为默认输入，复用 Day15 的 OES 纹理和 `CameraHelper`

### 滤镜架构策略
- **D-05:** Day 16 创建 `BaseFilterRenderer` 标准基类，实现 `GLSurfaceView.Renderer`
- **D-06:** 基类包含完整渲染管线：顶点缓冲、着色器加载、MVP 矩阵链、纹理绑定、双模式切换
- **D-07:** 子类只需提供：片段着色器代码（通过 `getFragmentShader()` 方法）、uniform 句柄、参数传递
- **D-08:** 统一顶点着色器支持 MVP + 纹理坐标，所有基础滤镜共用
- **D-09:** 参数更新策略：缓存参数值，只在值变化时调用 `glUniform1f()` 更新，减少不必要的 GL 调用

### UI 交互模式
- **D-10:** 纯 OpenGL 绘制 UI（深入学习 UI 渲染）
- **D-11:** 实现完整控件库：按钮、滑块、单选框、复选框、下拉菜单
- **D-12:** 布局位置：底部面板（类似相机 App，约占屏幕 20-30% 高度）
- **D-13:** 实施策略：Day 20 实现核心控件（按钮、滑块），Day 21 扩展其他控件
- **D-14:** UI 与滤镜内容分层渲染（先绘制滤镜，再绘制 UI）
- **D-15:** 触摸事件处理：检测点击/拖拽，映射到 UI 控件

### 代码规范与标准
- **D-16:** 统一采用 Day15 的着色器错误检查模式（完整错误检查，非静默失败）
- **D-17:** 添加 `glDisableVertexAttribArray()` 清理，避免状态泄漏
- **D-18:** 定义 `WORLD_HALF_SIZE` 常量，避免硬编码投影边界
- **D-19:** 使用 `Matrix.orthoM()` 多行格式（非压缩单行）
- **D-20:** 为每个滤镜添加详细 KDoc 注释（遵循 Day1-5 风格）

### Claude's Discretion
- 程序化纹理的具体图案选择（棋盘格、渐变等）由实现时决定
- UI 控件的视觉样式（颜色、圆角、阴影）由实现时决定
- 底部面板的具体高度比例（20-30% 范围内）由实现时决定

## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### 项目规范
- `AGENTS.md` — 项目编码规范、OpenGL 特定注释要求、正交投影矩阵规范
- `.planning/codebase/CONVENTIONS.md` — 代码库编码约定、命名规范、文件组织

### 需求文档
- `.planning/REQUIREMENTS.md` — BASIC_FILTER-01 to BASIC_FILTER-05 需求定义
- `.planning/ROADMAP.md` — Phase 3 目标和成功标准

### 参考实现
- `app/src/main/java/com/example/glearning/Day15Renderer.kt` — OES 纹理、着色器错误检查、相机集成
- `app/src/main/java/com/example/glearning/Day14Renderer.kt` — 纹理变换、多纹理混合、程序化纹理
- `app/src/main/java/com/example/glearning/Day10Renderer.kt` — 纹理基础、UV 坐标映射
- `app/src/main/java/com/example/glearning/CameraHelper.kt` — 相机辅助类

### 编码规范
- `AGENTS.md` §正交投影矩阵统一规范 — 所有使用 `Matrix.orthoM` 的渲染器必须遵循
- `AGENTS.md` §注释规范 — 所有生成的代码必须包含详细注释

## Existing Code Insights

### Reusable Assets
- **Day15Renderer.kt**: OES 外部纹理创建和绑定、`CameraHelper` 集成、着色器完整错误检查 — 滤镜双模式的基础
- **Day14Renderer.kt**: 程序化纹理生成（棋盘格、渐变、砖墙等）、纹理变换矩阵、多纹理混合模式 — 默认输入源
- **Day10Renderer.kt**: 2D 纹理创建标准流程、UV 坐标映射 — 纹理基础
- **Day9Renderer.kt**: MVP 矩阵链组合、多物体渲染 — 矩阵处理基础
- **Day4Renderer.kt**: 正交投影矩阵、`WORLD_HALF_SIZE` 常量 — 屏幕适配

### Established Patterns
- **Renderer 结构**: `Day<N>Renderer` 实现 `GLSurfaceView.Renderer`，`companion object` 存放常量和着色器代码
- **顶点数据格式**: `[x, y, u, v]` 交错存储，每顶点 4 个浮点数
- **缓冲区创建**: `ByteBuffer.allocateDirect()` + `ByteOrder.nativeOrder()` + `asFloatBuffer()`
- **着色器管理**: 顶点/片段着色器作为 `private const val` 字符串，`loadShader()` 方法编译
- **矩阵变换**: `Matrix.setIdentityM()` → `translateM/rotateM/scaleM()` → `multiplyMM()` 链式调用
- **动画模式**: `System.currentTimeMillis()` 计算 elapsedSeconds，正弦函数驱动动画

### Integration Points
- **MainActivity.kt**: `createRenderer(day)` when 表达式 — 需要添加 Day16-21 的 case
- **MainActivity.kt**: `showRendererSelector()` — 需要添加新渲染器的菜单项
- **GLSurfaceView**: 触摸事件通过 `QueueEvent()` 安全传递到渲染线程
- **CameraHelper.kt**: 相机预览管理 — 滤镜双模式需要复用

## Specific Ideas

- 用户希望在学习滤镜算法时使用已知内容的程序化纹理，便于调试和理解
- 用户希望最终目标是特效相机 App，因此需要尽早验证滤镜在真实相机画面上的效果
- 用户选择提前学习架构设计（Day 16 创建基类），而非等到 Day 19 再抽象
- 用户选择纯 OpenGL 绘制 UI，希望深入学习 UI 渲染而非使用 Android View 叠加层
- 用户希望实现完整控件库，可能需要 Day 20-21 之外的时间或较高实现效率

## Deferred Ideas

- 性能优化和内存管理（v2 需求，Phase 4 之后考虑）
- 更多滤镜效果如漫画风、水彩风（v2 需求）
- 滤镜参数预设保存功能（v2 需求）
- FBO 离屏渲染（Phase 4 内容，Day 24）

---

*Phase: 3-基础滤镜架构*
*Context gathered: 2026-05-11*
