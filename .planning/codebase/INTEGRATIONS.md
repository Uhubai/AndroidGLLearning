# Integrations & External Services

**Analysis Date:** 2025-07-09

## External APIs

**Android Platform APIs (on-device only):**

| API | Purpose | Auth | Usage |
|---|---|---|---|
| `android.opengl.GLES20` | OpenGL ES 2.0 rendering | None | All renderers — core graphics pipeline |
| `android.opengl.GLES11Ext` | OES external texture extension | None | `Day15Renderer` — camera texture binding |
| `android.opengl.Matrix` | 4×4 matrix math operations | None | Day 4+ — orthoM, multiplyMM, setLookAtM, rotateM, translateM |
| `android.opengl.GLUtils` | Bitmap-to-texture loading | None | Day 10+ — texImage2D |
| `android.hardware.camera2` | Camera device access & preview | Runtime permission (`CAMERA`) | `CameraHelper` + `Day15Renderer` |
| `android.graphics.SurfaceTexture` | Camera frame → GL texture bridge | None | `Day15Renderer` — OES texture from camera |
| `android.graphics.Bitmap` | Programmatic texture generation | None | Day 10–14 — createCheckerboardTexture, createGradientTexture |

**No network APIs or remote services are used.** This is a fully offline, on-device application.

## Third-Party Services

**None.** The project has zero external service integrations. All functionality uses:
- Android platform APIs (OpenGL ES, Camera2)
- AndroidX libraries (AppCompat, Core KTX, Material, ConstraintLayout)

No analytics, crash reporting, push notifications, or cloud services are integrated.

## Data Sources

**On-Device Data Only:**

| Source | Format | Access Pattern | Files |
|---|---|---|---|
| Programmatic vertex data | `FloatArray` in companion objects | Hardcoded in renderer classes | `Day2Renderer.kt`, `Day3Renderer.kt`, etc. |
| Programmatic index data | `ShortArray` in companion objects | Hardcoded in renderer classes | `Day3Renderer.kt`, `Day5Renderer.kt`, etc. |
| Procedural textures | `Bitmap` created in code | Generated at runtime via `Bitmap.createBitmap()` | `Day10Renderer.kt`, `Day12Renderer.kt` |
| Camera frames | `SurfaceTexture` → OES texture | Frame-by-frame via `onFrameAvailable` callback | `Day15Renderer.kt` |
| System time | `System.currentTimeMillis()` | Polled each frame for animation | All animated renderers |

**No external data files, no asset loading, no resource-based textures.** All vertex data and textures are generated programmatically in Kotlin code. No `.obj`, `.png`, `.jpg`, or other resource files are used for 3D content.

## Device Hardware Integration

**Camera2 Pipeline (Day 15):**

```text
CameraDevice → CaptureSession → Surface (from SurfaceTexture) → OES Texture → GLSL Shader → Screen
```

- `CameraHelper` (`CameraHelper.kt`) manages:
  - `CameraManager` → enumerate cameras, find back camera
  - `CameraDevice` → open/close camera via `StateCallback`
  - `CameraCaptureSession` → start/stop preview via `setRepeatingRequest`
  - Preview resolution: 640×480 (hardcoded default)
- `Day15Renderer` (`Day15Renderer.kt`) manages:
  - OES texture creation via `GLES11Ext.GL_TEXTURE_EXTERNAL_OES`
  - `SurfaceTexture` creation from OES texture ID
  - Frame update via `surfaceTexture.updateTexImage()` in `onDrawFrame`
  - UV correction via `surfaceTexture.getTransformMatrix()` → `u_TextureTransform` uniform

**Permission Model:**
- `CAMERA` permission requested at runtime via `ActivityResultContracts.RequestPermission`
- `android.hardware.camera` feature declared but `android:required="false"`
- Permission check in `CameraHelper.hasCameraPermission()` and `MainActivity.checkCameraPermission()`

## Build/CI Integrations

**None detected.**

- No CI/CD pipeline (no GitHub Actions, GitLab CI, etc.)
- No automated build signing or distribution
- No ProGuard/R8 optimization (minify disabled)
- No automated testing infrastructure
- Build uses Tencent mirror for Gradle distribution (`mirrors.cloud.tencent.com`)

## OpenGL ES Extension Dependencies

| Extension | Purpose | Required By |
|---|---|---|
| `GL_OES_EGL_image_external` | External texture sampling (`samplerExternalOES`) | `Day15Renderer` fragment + vertex shaders |

This extension is universally available on Android devices with OpenGL ES 2.0+ support.

## AndroidX Activity Result API

- **Usage:** Camera permission request in `MainActivity`
- **Implementation:** `registerForActivityResult(ActivityResultContracts.RequestPermission())`
- **File:** `MainActivity.kt:30-38`

## Logging

- **Framework:** `android.util.Log` (standard Android logging)
- **Usage:** `CameraHelper` and `Day15Renderer` only
  - `CameraHelper`: Log.d/w/e for camera lifecycle events
  - `Day15Renderer`: Log.d/e for shader compilation and renderer state
- **Tags:** `"CameraHelper"`, `"Day15Renderer"`
- **Other renderers (Day 1–14):** No logging — companion `TAG` constants defined but unused

---

*Integration audit: 2025-07-09*
