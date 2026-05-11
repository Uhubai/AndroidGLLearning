# Android OpenGL ES 特效相机学习项目

## What This Is

这是一个 **Android OpenGL ES 学习项目**，通过 38 天的系统性练习，从零开始构建一个**特效相机 App**。项目采用每日一个渲染器的方式，循序渐进地学习 OpenGL 渲染管线、矩阵变换、纹理、滤镜、人脸贴纸等核心技术。

当前已完成 Day 1-15 的基础实现，涵盖渲染管线、着色器、VBO、正交投影、矩阵变换、纹理等核心概念。下一步将进入相机纹理和基础滤镜阶段。

## Core Value

通过每日实践掌握 OpenGL ES 在 Android 上的实际应用，最终能够独立开发带实时滤镜和人脸贴纸的特效相机应用。

## Requirements

### Validated

- ✓ 项目搭建 + 渲染管线理解（Day 1）— existing
- ✓ 顶点着色器 + 三角形绘制（Day 2）— existing
- ✓ VBO + 索引缓冲（Day 3-4）— existing
- ✓ 正交投影 + 屏幕适配（Day 4-5）— existing
- ✓ 平移矩阵 + 动画（Day 5-6）— existing
- ✓ 旋转矩阵 + 动画（Day 8）— existing
- ✓ MVP 矩阵组合 + 多物体渲染（Day 9）— existing
- ✓ UV 坐标 + 纹理贴图（Day 10-11）— existing
- ✓ 多纹理叠加 + 混合模式（Day 12-13）— existing
- ✓ SurfaceTexture + OES 外部纹理（Day 15）— existing

### Active

- [ ] Day 16-18: 基础滤镜（亮度/对比度/饱和度/灰度/反色/Sepia/色调/色温）
- [ ] Day 19: 滤镜架构设计（可切换滤镜管理器）
- [ ] Day 20-21: 滤镜面板 UI + 参数调节
- [ ] Day 22-23: 卷积滤镜（模糊/边缘检测/锐化）
- [ ] Day 24: FBO + 多 pass 渲染
- [ ] Day 25-26: LUT 滤镜 + 美颜滤镜
- [ ] Day 27-28: 高级滤镜组合复习
- [ ] Day 29-33: 贴纸特效（多纹理混合/人脸检测/动态贴纸/3D 贴纸）
- [ ] Day 34: 录制与保存（MediaCodec + OpenGL）
- [ ] Day 35-38: 综合项目开发（特效相机 App 完整实现）

### Out of Scope

- iOS 或其他平台支持 — 专注 Android
- 第三方渲染引擎 — 使用原生 OpenGL ES
- 复杂的 3D 建模 — 仅涉及基础 3D 贴纸

## Context

### 学习背景
- **时长**：5 周（38 天），每日约 1 小时
- **背景**：了解 OpenGL 概念，无实践经验，数学需复习，Camera2 经验丰富
- **学习方式**：每日一个 Day<N>Renderer，渐进式学习

### 技术环境
- **平台**：Android (Kotlin)
- **图形 API**：OpenGL ES 2.0/3.0
- **相机 API**：Camera2 API
- **构建工具**：Gradle (Kotlin DSL)
- **操作系统**：Windows 11
- **终端**：PowerShell 7

### 当前进度
- **已完成**：Day 1-15（基础渲染、矩阵变换、纹理、相机预览）
- **进行中**：准备进入 Day 16（基础滤镜）
- **代码结构**：16 个 Kotlin 文件，单一包结构 `com.example.glearning`

## Constraints

- **时间**：每日约 1 小时学习时间
- **技术栈**：Kotlin + OpenGL ES（不使用第三方渲染库）
- **设备兼容性**：支持 OpenGL ES 2.0+ 的 Android 设备
- **学习节奏**：循序渐进，每个概念通过实践掌握

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| 使用 Day<N>Renderer 命名 | 清晰的学习进度追踪 | ✓ Good — 便于管理和复习 |
| 单一包结构 | 初期简单，避免过度设计 | ⚠️ Revisit — 后期可能需要重构为子包 |
| Day 6 映射到 Day7Renderer | 复习日不需要独立渲染器 | ✓ Good — 合理简化 |
| Kotlin 语言选择 | 现代 Android 开发标准 | ✓ Good — 符合行业趋势 |
| 正交投影提前完成 | 实际需求驱动学习节奏 | ✓ Good — 灵活调整计划 |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-05-11 after project initialization with gsd-new-project*
