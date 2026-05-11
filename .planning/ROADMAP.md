# Roadmap

## Overview

- **Total Phases**: 4
- **Current Phase**: 4 (基础滤镜与特效)
- **Requirements Coverage**: 100% (v1 requirements mapped)

---

### Phase 1: 基础渲染与矩阵变换
**Goal**: 掌握 OpenGL 渲染管线、着色器、VBO、矩阵变换
**Success Criteria**:
1. 能够独立绘制三角形、矩形等基本图形
2. 理解并实现正交投影和屏幕适配
3. 实现平移、旋转等矩阵变换动画
4. 掌握 MVP 矩阵组合和多物体渲染

**Status**: ✓ Complete (Day 1-13)

---

### Phase 2: 纹理与相机预览
**Goal**: 掌握纹理贴图、UV 坐标、相机预览集成
**Success Criteria**:
1. 能够实现纹理贴图和 UV 动画
2. 理解并实现多纹理混合
3. 成功集成 Camera2 API 到 GLSurfaceView
4. 使用 OES 外部纹理显示相机预览

**Status**: ✓ Complete (Day 10-15)

---

### Phase 3: 基础滤镜架构
**Goal**: 实现可调节的基础滤镜和滤镜管理架构
**Success Criteria**:
1. 实现亮度/对比度/饱和度可调滤镜
2. 实现灰度/反色/Sepia 经典滤镜
3. 实现色调/色温调节
4. 设计可切换的滤镜管理器架构
5. 创建滤镜参数调节 UI

**Requirements**: BASIC_FILTER-01 to BASIC_FILTER-05
**Mode**: standard
**Status**: ⬜ Not Started

---

### Phase 4: 高级滤镜与特效
**Goal**: 实现卷积滤镜、FBO、LUT、美颜、贴纸和录制功能
**Success Criteria**:
1. 实现高斯模糊和边缘检测等卷积滤镜
2. 使用 FBO 实现多 pass 渲染和滤镜叠加
3. 实现 3D LUT 专业滤镜
4. 实现美颜磨皮效果
5. 集成 ML Kit 人脸检测
6. 实现动态贴纸跟随人脸
7. 实现带特效的视频录制
8. 完成特效相机 App 完整实现

**Requirements**: ADVANCED_FILTER-01 to ADVANCED_FILTER-06, STICKER-01 to STICKER-05, RECORDING-01 to RECORDING-02
**Mode**: standard
**Status**: ⬜ Not Started

---

## Phase Dependencies

```
Phase 1 (基础渲染) → Phase 2 (纹理与相机) → Phase 3 (基础滤镜) → Phase 4 (高级特效)
```

每个阶段依赖前一阶段的基础概念，必须按顺序完成。

## Notes

- 当前进度：Day 15 完成（相机预览）
- 下一步：Day 16 开始基础滤镜
- 学习方式：每日一个 Day<N>Renderer，渐进式实现
- 复习日（Day 20-21, 27-28, 35-38）用于巩固和综合练习

---
*Last updated: 2026-05-11 after roadmap creation*
