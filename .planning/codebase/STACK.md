# Technology Stack

**Analysis Date:** 2025-07-09

## Runtime Platform

- **Android** — API 24 (Android 7.0 Nougat) minimum, API 34 (Android 14) target
- **JVM Target:** Java 8 (sourceCompatibility/targetCompatibility = VERSION_1_8, jvmTarget = "1.8")

## Languages

- **Kotlin 1.9.20** — Primary and only language for all source files (16 `.kt` files)
  - Uses Kotlin standard library extensions (`kotlin.math.sin`, `kotlin.math.cos`, `kotlin.math.PI`)
  - No Java source files in the project
- **GLSL (OpenGL ES 2.0 Shading Language)** — Embedded as string literals in Kotlin renderer classes
  - Vertex shaders and fragment shaders defined as `const val` triple-quoted strings in companion objects

## Frameworks & Libraries

| Framework/Library | Version | Purpose | Scope |
|---|---|---|---|
| Android SDK | compileSdk 34, minSdk 24, targetSdk 34 | Platform APIs (Activity, Camera2, View, etc.) | Core |
| AndroidX AppCompat | 1.6.1 | AppCompatActivity, AlertDialog, backward-compatible UI | Core |
| AndroidX Core KTX | 1.12.0 | Kotlin extensions for Android APIs | Core |
| Material Components | 1.11.0 | Theme.MaterialComponents.DayNight.NoActionBar, UI widgets | Peripheral |
| AndroidX ConstraintLayout | 2.1.4 | Layout management (used in activity_main.xml) | Peripheral |
| AndroidX Activity | (transitive via AppCompat) | ActivityResultContracts for permission requests | Core |
| OpenGL ES 2.0 | API level 24+ | 3D graphics rendering via GLES20, GLSurfaceView | Core |
| Camera2 API | API level 24+ | Camera device access for Day 15 renderer | Core (Day 15) |

## Build System

- **Gradle 8.2** — Build automation
  - Distribution URL: `https://mirrors.cloud.tencent.com/gradle/gradle-8.2-bin.zip` (Tencent mirror)
- **Android Gradle Plugin (AGP)** 8.2.0 — Android build plugin
- **Kotlin Android Plugin** 1.9.20 — Kotlin compilation support
- **Gradle Properties:**
  - `org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8`
  - `android.useAndroidX=true`
  - `kotlin.code.style=official`
  - `android.nonTransitiveRClass=true`
- **Build Types:** Debug and Release (release has minify disabled)
- **ProGuard:** `proguard-android-optimize.txt` + `proguard-rules.pro` (not active — minify disabled)

## Key Dependencies

| Dependency | Version | Purpose | Scope |
|---|---|---|---|
| `androidx.core:core-ktx` | 1.12.0 | Kotlin extensions for Context, Permission checks | Core |
| `androidx.appcompat:appcompat` | 1.6.1 | AppCompatActivity base class, AlertDialog | Core |
| `com.google.android.material:material` | 1.11.0 | Material Design themes and components | Peripheral |
| `androidx.constraintlayout:constraintlayout` | 2.1.4 | UI layout constraints | Peripheral |
| `android.opengl.GLES20` | Platform | OpenGL ES 2.0 rendering API | Core |
| `android.opengl.GLES11Ext` | Platform | OES external texture extension (camera) | Core (Day 15) |
| `android.opengl.Matrix` | Platform | 4×4 matrix math (orthoM, multiplyMM, setLookAtM, etc.) | Core |
| `android.opengl.GLUtils` | Platform | texImage2D for loading Bitmap into GL texture | Core (Day 10+) |
| `android.hardware.camera2` | Platform | Camera2 API for camera preview | Core (Day 15) |
| `android.graphics.SurfaceTexture` | Platform | Capture camera frames as GL textures | Core (Day 15) |
| `java.nio.*` | Platform | Direct ByteBuffer, FloatBuffer, ShortBuffer for GL data | Core |

## OpenGL ES Capabilities Used

- **OpenGL ES 2.0** — Set via `GLSurfaceView.setEGLContextClientVersion(2)`
- **GLSL ES 1.0** — Shader language (compatible with GLES 2.0)
  - `attribute` variables for per-vertex data
  - `uniform` variables for per-draw data (matrices, textures)
  - `varying` variables for vertex-to-fragment interpolation
  - `sampler2D` for standard 2D textures
  - `samplerExternalOES` with `GL_OES_EGL_image_external` extension for camera textures
- **Render Modes:**
  - `RENDERMODE_CONTINUOUSLY` — Days 1–14 (continuous animation)
  - `RENDERMODE_WHEN_DIRTY` — Day 15 (camera, frame-driven)

## Dev Tools

- **No linter/formatter configuration detected** — No `.eslintrc`, `.prettierrc`, `biome.json`, `ktlint`, or `detekt` config files
- **No test framework** — No test dependencies declared, no test source sets
- **No CI/CD configuration** — No `.github/workflows`, `Jenkinsfile`, or similar
- **Android Manifest Declarations:**
  - `android.permission.CAMERA` permission
  - `android.hardware.camera` feature (not required)

## Configuration

**Environment:**
- Android SDK with compileSdk 34 required
- Tencent mirror used for Gradle distribution (implies Chinese network environment)

**Build:**
- `build.gradle.kts` (root) — Plugin declarations
- `app/build.gradle.kts` — Android config, dependencies
- `settings.gradle.kts` — Repositories, project name ("GLearning")
- `gradle.properties` — JVM args, AndroidX flags

## Platform Requirements

**Development:**
- Android Studio (recommended for AGP 8.2.0 compatibility)
- Android SDK Platform 34
- JDK 8+ (JVM target 1.8)
- Kotlin plugin 1.9.20

**Production:**
- Android device/emulator running API 24+ (Android 7.0+)
- OpenGL ES 2.0 capable GPU (virtually all Android devices)
- Camera hardware (optional — only required for Day 15)

---

*Stack analysis: 2025-07-09*
