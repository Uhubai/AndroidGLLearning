---
plan_id: 03
objective: 实现 Day17Renderer - 灰度/反色/Sepia 经典滤镜
status: complete
---

# Plan 03 Summary

## What Was Built

Day17Renderer 实现三种经典滤镜：灰度、反色、Sepia，通过 uniform int 切换。

## Key Files

- `app/src/main/java/com/example/glearning/Day17Renderer.kt`

## Implementation

- 灰度：`dot(color.rgb, vec3(0.299, 0.587, 0.114))`
- 反色：`1.0 - color.rgb`
- Sepia：固定矩阵变换 RGB 值
- 滤镜切换：`u_FilterType` uniform int 选择器

## Self-Check: PASSED
