# Coding Conventions

**Analysis Date:** 2025-07-11

## Naming Conventions

### Classes
- **Pattern:** PascalCase with `Day<N>` prefix for renderers
- **Examples:** `Day1Renderer`, `Day2Renderer`, `Day15Renderer`, `CameraHelper`, `MainActivity`
- **Renderer naming:** Always `Day<N>Renderer` where N is 1-based integer; no Day6Renderer exists (Day 6 maps to Day7Renderer in `MainActivity.kt:139`)

### Methods
- **Pattern:** camelCase
- **Examples:** `onSurfaceCreated`, `onDrawFrame`, `loadShader`, `createTexture`, `drawRectangle`, `drawQuad`
- **OpenGL callback overrides:** Keep Android SDK naming (`onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`)
- **Private helper methods:** camelCase, descriptive (`createCheckerboardTexture`, `textureScrollAnimation`)

### Variables
- **Pattern:** camelCase for instance variables and locals
- **Examples:** `program`, `positionHandle`, `colorHandle`, `matrixHandle`, `vertexBuffer`, `startTime`
- **Matrix variables:** Always `FloatArray(16)`, named with suffix `Matrix` — `projectionMatrix`, `modelMatrix`, `viewMatrix`, `resultMatrix`, `tempMatrix`, `textureMatrix`
- **Buffer variables:** Suffixed with `Buffer` — `vertexBuffer`, `indexBuffer`, `repeatVertexBuffer`
- **Handle variables:** Suffixed with `Handle` — `positionHandle`, `colorHandle`, `matrixHandle`, `textureCoordHandle`

### Constants
- **Pattern:** UPPER_SNAKE_CASE in `companion object`
- **Shader code:** `VERTEX_SHADER_CODE`, `FRAGMENT_SHADER_CODE`
- **Vertex attributes:** `COORDS_PER_VERTEX`, `COLORS_PER_VERTEX`, `UV_PER_VERTEX`, `FLOAT_SIZE`, `SHORT_SIZE`
- **World coordinates:** `WORLD_HALF_SIZE` (defined in `Day4Renderer.kt:41`, `Day5Renderer.kt:33`, `Day7Renderer.kt:38`, `Day8Renderer.kt:44`)
- **Geometry:** `OUTER_RADIUS`, `INNER_RADIUS`, `ROTATION_SPEED`
- **TAG constant:** `private const val TAG = "Day<N>Renderer"` in every companion object (though only Day15Renderer and CameraHelper actually use `Log`)

## File Organization

### Package Structure
- All source files in single package: `com.example.glearning`
- No sub-packages (renderer/, shader/, model/, util/ as suggested in AGENTS.md do not exist yet)
- 16 Kotlin files total, all co-located in `app/src/main/java/com/example/glearning/`

### File Naming
- **Pattern:** `<Class>.kt` — one public class per file
- **Renderer files:** `Day<N>Renderer.kt`
- **Helper files:** `CameraHelper.kt`
- **Activity files:** `MainActivity.kt`

### Code Within Files
- Class-level KDoc → package declaration → imports → class body
- `companion object` always first inside class (constants, shader code, vertex data)
- Instance variables grouped by purpose with section comments:
  ```kotlin
  // 着色器程序相关
  private var program: Int = 0
  
  // 顶点数据缓冲区
  private var vertexBuffer: FloatBuffer? = null
  
  // MVP 矩阵相关
  private val modelMatrix = FloatArray(16)
  
  // 动画相关
  private var startTime: Long = 0
  ```
- Method order: `onSurfaceCreated` → `onSurfaceChanged` → `onDrawFrame` → private helpers → `loadShader` (always last)

## Code Style

### Indentation & Formatting
- 4-space indentation (Kotlin standard)
- Trailing commas in array literals are inconsistent (Day2-3 have no trailing comma on last element, Day4+ sometimes omits)
- Maximum line length not enforced; some `glVertexAttribPointer` calls are long

### Braces
- Opening brace on same line (Kotlin convention)
- Always use braces for `if/else` blocks in `onSurfaceChanged` (orthoM projection blocks)

### Imports
- No explicit import ordering enforced (no .editorconfig or ktlint config)
- Day 1-8: Use fully qualified `android.opengl.Matrix.orthoM` (no import)
- Day 9+: Import `android.opengl.Matrix` and use `Matrix.orthoM`
- `Math.sin`/`Math.cos` used in Day5, Day11 vs `kotlin.math.sin`/`kotlin.math.cos` in Day7, Day12-14 (inconsistency)

### Nullable Types
- Buffer variables use `FloatBuffer?` and `ShortBuffer?` with safe calls (`vertexBuffer?.position(0)`)
- Some handles use `Int = 0` as sentinel instead of nullable (OpenGL convention — 0 is invalid handle)

### Variable Initialization
- All `var` handles initialized to `0`: `private var program: Int = 0`
- All matrix arrays initialized immediately: `private val projectionMatrix = FloatArray(16)`
- Buffers initialized to `null`, set in `onSurfaceCreated`

## Error Handling Patterns

### Shader Compilation
- Day 1-14: **No error checking** — `loadShader` compiles and returns without checking `GL_COMPILE_STATUS`
- Day 15: **Full error checking** — checks `GL_COMPILE_STATUS`, logs errors with `Log.e`, returns 0 on failure, checks `GL_LINK_STATUS` for program linking
- Pattern in `Day15Renderer.kt:129-143`:
  ```kotlin
  val compiled = IntArray(1)
  GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
  if (compiled[0] == GLES20.GL_FALSE) {
      Log.e(TAG, "着色器编译失败: ${GLES20.glGetShaderInfoLog(shader)}")
      GLES20.glDeleteShader(shader)
      return 0
  }
  ```

### Camera Errors
- `CameraHelper.kt` uses try-catch with `Log.e` for all Camera2 API calls
- Early return with `Log.w` for precondition failures (no permission, no surface)
- `Day15Renderer.kt` checks `surfaceTextureReady` flag before rendering

### OpenGL State Cleanup
- Day 1-9: Call `glDisableVertexAttribArray` after draw for each attribute
- Day 10-14: **Missing** `glDisableVertexAttribArray` calls (texture renderers don't clean up vertex attrib arrays)
- Day 15: No `glDisableVertexAttribArray` (uses `GL_TRIANGLE_STRIP` with no index buffer)

### Logging
- Only `CameraHelper.kt` and `Day15Renderer.kt` use `android.util.Log`
- All other renderers define `TAG` but never use `Log`
- Log levels: `Log.d` for lifecycle events, `Log.e` for errors, `Log.w` for warnings

## Documentation Conventions

### Class-Level KDoc (Strictly Followed)
Every renderer has a structured class-level comment with:
1. Title: `Day N: <Topic> - <Subtitle>`
2. `本渲染器演示：` numbered list of concepts
3. `绘制内容：` description of what's rendered
4. Additional explanation of key concepts

Example from `Day2Renderer.kt:1-17`:
```kotlin
/**
 * Day 2: 顶点着色器与片段着色器 - 绘制渐变色三角形
 *
 * 本渲染器演示：
 * 1. 顶点着色器（Vertex Shader）：处理顶点位置和属性
 * 2. 片段着色器（Fragment Shader）：计算每个像素的颜色
 * ...
 *
 * 绘制内容：一个带顶点颜色渐变的三角形
 */
```

### Method-Level KDoc
- Override methods (`onSurfaceCreated`, `onSurfaceChanged`, `onDrawFrame`) always documented
- Follows template: `执行时机:` + `主要任务:` numbered list
- Private helper methods have variable documentation (Day2-5 detailed, Day10+ minimal)
- `loadShader` documented in early days, undocumented in later renderers

### Inline Comments
- Very extensive — nearly every GL call has an inline comment
- Chinese language for all comments
- Pattern: explain "why" and parameter meaning, not just "what"
- Example: `// 参数范围：0.0f ~ 1.0f 表示颜色强度`
- Step numbering pattern: `// 步骤1：`, `// 步骤2：`, etc.

### Shader Code Documentation
- Each shader constant has a KDoc block explaining:
  - Variable types: `uniform`, `attribute`, `varying`
  - Workflow: numbered steps
  - Key concepts (interpolation, precision, etc.)
- Inline comments within shader strings are minimal

### Data Structure Documentation
- Vertex arrays always documented with:
  - Data format: `[x, y, r, g, b] × N` structure
  - ASCII art vertex layout diagram
  - Coordinate range explanation

## OpenGL-Specific Conventions

### Shader Code
- **Storage:** Embedded as `private const val` strings in `companion object` (not in `res/raw/` as AGENTS.md suggests)
- **Vertex shader naming:**
  - `u_Matrix` — combined transformation matrix (uniform)
  - `u_TextureMatrix` — texture transformation matrix (uniform, Day11+)
  - `u_TextureTransform` — OES texture transform from SurfaceTexture (uniform, Day15)
  - `a_Position` — vertex position (attribute)
  - `a_Color` — vertex color (attribute, Day2-9)
  - `a_TextureCoord` — UV coordinates (attribute, Day10+)
  - `v_Color` — interpolated color (varying)
  - `v_TextureCoord` — interpolated UV (varying)
- **Fragment shader naming:**
  - `u_Texture` / `u_Texture1` / `u_Texture2` — sampler uniforms
  - `u_MixFactor` — blend factor (uniform)
  - `u_BlendMode` — blend mode selector (uniform, Day12)
  - `u_UseSecondTexture` — toggle for second texture (uniform, Day14)
- **Precision:** Always `precision mediump float;` at top of fragment shader
- **OES extension:** Day15 requires `#extension GL_OES_EGL_image_external : require` in both vertex and fragment shaders

### Buffer Management
- **Direct buffers only:** Always `ByteBuffer.allocateDirect()` → `.order(ByteOrder.nativeOrder())` → `.asFloatBuffer()` / `.asShortBuffer()`
- **Interleaved vertex format:** Position + color or position + UV in single buffer
  - Color renderers: `[x, y, r, g, b]` per vertex (Day2-9)
  - Texture renderers: `[x, y, u, v]` per vertex (Day10+)
- **Stride calculation:** `val stride = (COORDS_PER_VERTEX + COLORS_PER_VERTEX) * FLOAT_SIZE`
- **Buffer positioning:** `vertexBuffer?.position(0)` for position data, `vertexBuffer?.position(COORDS_PER_VERTEX)` for color/UV offset

### Matrix Conventions
- **4×4 matrices:** Always `FloatArray(16)` (column-major as per OpenGL)
- **Matrix chain:** `resultMatrix = projectionMatrix × modelMatrix` (Day4-8)
- **MVP chain:** `tempMatrix = viewMatrix × modelMatrix`, `resultMatrix = projectionMatrix × tempMatrix` (Day9+)
- **Identity reset:** `Matrix.setIdentityM(modelMatrix, 0)` before each frame's transformations
- **Transform order in code:** Scale → Rotate → Translate (applied right-to-left: T × R × S × vertex)
- **`glUniformMatrix4fv` transpose param:** Always `false` (OpenGL ES requirement)

### Orthographic Projection (Critical Convention)
- **World coordinate system:** Vertices use world coordinates (±100 or ±150), NOT NDC
- **`WORLD_HALF_SIZE` constant:** Must be defined and match vertex range + animation range
  - Day 4, 5, 8: `100f` (rectangle vertices ±100, no/limited animation)
  - Day 7: `150f` (star vertices ±80, with scale/translate animation needing more room)
  - Day 9-14: Hardcoded `100f` or `150f` without `WORLD_HALF_SIZE` constant
- **Projection formula (MUST follow):**
  ```kotlin
  if (aspectRatio > 1f) {
      // Landscape: extend horizontal
      Matrix.orthoM(projectionMatrix, 0,
          -aspectRatio * WORLD_HALF_SIZE, aspectRatio * WORLD_HALF_SIZE,
          -WORLD_HALF_SIZE, WORLD_HALF_SIZE,
          -1f, 1f)
  } else {
      // Portrait: extend vertical
      Matrix.orthoM(projectionMatrix, 0,
          -WORLD_HALF_SIZE, WORLD_HALF_SIZE,
          -WORLD_HALF_SIZE / aspectRatio, WORLD_HALF_SIZE / aspectRatio,
          -1f, 1f)
  }
  ```
- **Near/far planes:** Always `-1f, 1f` for 2D orthographic

### Drawing Pattern
- **Indexed drawing (Day3+):** `GLES20.glDrawElements(GL_TRIANGLES, indices.size, GL_UNSIGNED_SHORT, indexBuffer)`
- **Direct drawing (Day2):** `GLES20.glDrawArrays(GL_TRIANGLES, 0, vertexCount)`
- **Triangle strip (Day15):** `GLES20.glDrawArrays(GL_TRIANGLE_STRIP, 0, 4)`
- **Index pattern for rectangle:** `shortArrayOf(0, 1, 2, 1, 3, 2)` (two triangles, counter-clockwise winding)

### Texture Conventions
- **Creation pattern:** `glGenTextures` → `glBindTexture` → `glTexParameteri` (min/mag filter, wrap S/T) → `GLUtils.texImage2D` → `bitmap.recycle()` → `glBindTexture(GL_TEXTURE_2D, 0)`
- **Default parameters:** `GL_LINEAR` for min/mag filter, `GL_CLAMP_TO_EDGE` for wrap
- **Repeat textures:** `GL_REPEAT` for wrap S/T (Day11, Day14 brick texture)
- **Procedural textures:** All textures generated programmatically (no resource files)

### Animation Conventions
- **Time source:** `System.currentTimeMillis()` with `startTime` initialized in `onSurfaceCreated`
- **Elapsed calculation:** `val elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000f`
- **Sinusoidal motion:** `Math.sin(elapsedSeconds * frequency).toFloat() * amplitude` or `sin(elapsedSeconds * frequency) * amplitude`
- **Lissajous curves:** Different frequencies for X and Y (e.g., `sin(t * 2.0) * 100`, `cos(t * 3.0) * 50` in Day5)

## Inconsistencies Found

### 1. Matrix Import Style
- **Day 4, 5, 7, 8:** Use fully qualified `android.opengl.Matrix.orthoM()` (no import)
- **Day 9-15:** Import `android.opengl.Matrix` and use `Matrix.orthoM()`
- **Impact:** Visual inconsistency; imported version is preferred (shorter, cleaner)
- **Files:** `Day4Renderer.kt`, `Day5Renderer.kt`, `Day7Renderer.kt`, `Day8Renderer.kt`

### 2. Math Function Import Style
- **Day 5, 10, 11:** Use `Math.sin()`, `Math.cos()`, `Math.sqrt()`, `Math.atan2()` (Java style)
- **Day 7, 12, 13, 14:** Use `kotlin.math.sin`, `kotlin.math.cos` (Kotlin style) — sometimes with explicit `kotlin.math.` prefix
- **Impact:** Mixed style within same codebase; Kotlin math should be preferred
- **Files:** `Day5Renderer.kt:269-270`, `Day10Renderer.kt:23-24`, `Day11Renderer.kt:141-142`

### 3. WORLD_HALF_SIZE Constant Usage
- **Day 4, 5, 7, 8:** Define `WORLD_HALF_SIZE` constant and use it in `orthoM`
- **Day 9, 10:** Hardcode `100f` directly in `orthoM` calls without `WORLD_HALF_SIZE`
- **Day 11, 12, 13, 14:** Hardcode `150f` without constant
- **Impact:** Violates AGENTS.md convention; magic numbers make projection harder to understand
- **Files:** `Day9Renderer.kt:211-213`, `Day10Renderer.kt:328-341`, `Day11Renderer.kt:313-325`

### 4. Comment Density
- **Day 1-5, 7-8:** Extremely detailed comments (KDoc on every method, inline comments on every GL call)
- **Day 10-14:** Reduced comments (no KDoc on `loadShader`, fewer inline comments, shorter section comments)
- **Day 13, 14:** Most sparse — `复习` comment style, compressed code formatting
- **Impact:** Later renderers don't follow the detailed comment convention from AGENTS.md
- **Files:** `Day13Renderer.kt`, `Day14Renderer.kt`

### 5. Shader Error Checking
- **Day 1-14:** No shader compilation or link error checking
- **Day 15:** Full error checking with `glGetShaderiv`, `glGetProgramiv`, `Log.e`
- **Impact:** Silent failures in Day 1-14; only Day 15 is robust
- **Files:** All `loadShader` methods in Day1-14 renderers

### 6. Missing glDisableVertexAttribArray
- **Day 2-9:** Always call `glDisableVertexAttribArray` after drawing
- **Day 10-14:** Missing these cleanup calls (texture renderers)
- **Impact:** Potential GL state leaks between draw calls when rendering multiple objects
- **Files:** `Day10Renderer.kt`, `Day11Renderer.kt`, `Day12Renderer.kt`, `Day13Renderer.kt`, `Day14Renderer.kt`

### 7. Vertex Data Naming
- **Day 2-3:** `TRIANGLE_COORDS_AND_COLORS` / `RECT_COORDS_AND_COLORS`
- **Day 4-9:** `RECT_COORDS_AND_COLORS` (with world coordinates)
- **Day 10-14:** `RECT_COORDS_AND_UVS` (position + UV instead of position + color)
- **Day 15:** `FULL_SCREEN_RECT` (position + UV, NDC coordinates)
- **Impact:** Consistent naming within each data format type

### 8. Variable Comment Style
- **Day 2-8:** Inline comments on variable declarations: `private var program: Int = 0  // 着色器程序 ID`
- **Day 9-14:** No inline comments on variable declarations
- **Impact:** Later renderers lose the self-documenting convention
- **Files:** `Day9Renderer.kt` through `Day14Renderer.kt`

### 9. View Matrix Eye Distance
- **Day 9:** `setLookAtM` with eye at `(0, 0, 3)` — `viewMatrix` initialized in `onSurfaceCreated`
- **Day 10-14:** `setLookAtM` with eye at `(0, 0, 5)` — different Z distance
- **Impact:** Different effective zoom levels; Day 9 appears slightly more zoomed in
- **Files:** `Day9Renderer.kt:154`, `Day10Renderer.kt:216`, `Day11Renderer.kt:238`

### 10. Compressed Code Formatting
- **Day 13, 14:** Some methods use compressed single-line formatting:
  ```kotlin
  Matrix.orthoM(projectionMatrix, 0, -aspectRatio * 150f, aspectRatio * 150f, -150f, 150f, -1f, 1f)
  ```
- **Day 4-8:** Same call formatted across multiple lines with parameter comments
- **Impact:** Later renderers sacrifice readability for brevity
- **Files:** `Day13Renderer.kt:240-243`, `Day14Renderer.kt:281-284`

---

*Convention analysis: 2025-07-11*
