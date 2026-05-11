# Requirements

## v1 Requirements

### 基础渲染 (RENDER)
- [x] **RENDER-01**: 项目搭建 + 渲染管线理解 — Day 1 完成
- [x] **RENDER-02**: 顶点着色器 + 三角形绘制 — Day 2 完成
- [x] **RENDER-03**: VBO + 索引缓冲 — Day 3-4 完成
- [x] **RENDER-04**: 正交投影 + 屏幕适配 — Day 4-5 完成

### 矩阵变换 (TRANSFORM)
- [x] **TRANSFORM-01**: 平移矩阵 + 动画 — Day 5-6 完成
- [x] **TRANSFORM-02**: 旋转矩阵 + 动画 — Day 8 完成
- [x] **TRANSFORM-03**: MVP 矩阵组合 + 多物体渲染 — Day 9 完成

### 纹理 (TEXTURE)
- [x] **TEXTURE-01**: UV 坐标 + 纹理贴图 — Day 10-11 完成
- [x] **TEXTURE-02**: 多纹理叠加 + 混合模式 — Day 12-13 完成
- [x] **TEXTURE-03**: SurfaceTexture + OES 外部纹理 — Day 15 完成

### 基础滤镜 (BASIC_FILTER)
- [ ] **BASIC_FILTER-01**: 亮度/对比度/饱和度调节 — Day 16
- [ ] **BASIC_FILTER-02**: 灰度/反色/Sepia 滤镜 — Day 17
- [ ] **BASIC_FILTER-03**: 色调/色温调节 — Day 18
- [ ] **BASIC_FILTER-04**: 滤镜架构设计（可切换管理器）— Day 19
- [ ] **BASIC_FILTER-05**: 滤镜面板 UI + 参数调节 — Day 20-21

### 高级滤镜 (ADVANCED_FILTER)
- [ ] **ADVANCED_FILTER-01**: 卷积滤镜（模糊）— Day 22
- [ ] **ADVANCED_FILTER-02**: 边缘检测/锐化 — Day 23
- [ ] **ADVANCED_FILTER-03**: FBO + 多 pass 渲染 — Day 24
- [ ] **ADVANCED_FILTER-04**: LUT 滤镜 — Day 25
- [ ] **ADVANCED_FILTER-05**: 美颜滤镜 — Day 26
- [ ] **ADVANCED_FILTER-06**: 高级滤镜组合 — Day 27-28

### 贴纸特效 (STICKER)
- [ ] **STICKER-01**: 多纹理混合（Logo/贴纸叠加）— Day 29
- [ ] **STICKER-02**: 人脸检测集成（ML Kit）— Day 30
- [ ] **STICKER-03**: 动态贴纸定位（跟随人脸）— Day 31
- [ ] **STICKER-04**: 动态贴纸动画（眨眼/张嘴触发）— Day 32
- [ ] **STICKER-05**: 3D 贴纸基础（透视投影）— Day 33

### 录制与综合 (RECORDING)
- [ ] **RECORDING-01**: 录制带特效视频（MediaCodec + OpenGL）— Day 34
- [ ] **RECORDING-02**: 特效相机 App 完整实现 — Day 35-38

## v2 Requirements (Deferred)

- [ ] 性能优化和内存管理
- [ ] 更多滤镜效果（漫画风、水彩风等）
- [ ] 分享功能（社交媒体集成）
- [ ] 用户自定义滤镜参数预设

## Out of Scope

- iOS 或其他平台支持 — 专注 Android 平台
- 第三方渲染引擎（Unity/Unreal）— 使用原生 OpenGL ES
- 复杂的 3D 建模 — 仅涉及基础 3D 贴纸
- 视频后期处理 — 专注实时渲染

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| RENDER-01 to RENDER-04 | Phase 1 | ✓ Validated |
| TRANSFORM-01 to TRANSFORM-03 | Phase 2 | ✓ Validated |
| TEXTURE-01 to TEXTURE-03 | Phase 3 | ✓ Validated |
| BASIC_FILTER-01 to BASIC_FILTER-05 | Phase 4 | Active |
| ADVANCED_FILTER-01 to ADVANCED_FILTER-06 | Phase 5 | Active |
| STICKER-01 to STICKER-05 | Phase 6 | Active |
| RECORDING-01 to RECORDING-02 | Phase 7 | Active |

---
*Last updated: 2026-05-11 after requirements definition*
