---
wave: 1
depends_on: []
objective: 创建 BaseFilterRenderer 基类，封装完整渲染管线
requirements_addressed: [BASIC_FILTER-04]
files_modified:
  - app/src/main/java/com/example/glearning/BaseFilterRenderer.kt
autonomous: true
---

# Plan 01: BaseFilterRenderer 基类

## Context

Day 16 需要创建 `BaseFilterRenderer` 标准基类，实现 `GLSurfaceView.Renderer`，封装完整渲染管线。子类只需提供片段着色器代码和 uniform 参数。

**关键决策**:
- D-05: Day 16 创建 BaseFilterRenderer 标准基类
- D-06: 基类包含完整渲染管线
- D-07: 子类只需提供片段着色器代码
- D-08: 统一顶点着色器支持 MVP + 纹理坐标
- D-09: 参数变化时才更新 uniform
- D-01 to D-04: 双模式策略（程序化纹理 + 相机预览）
- D-16 to D-20: 代码规范与标准

## Tasks

### Task 1: 创建 BaseFilterRenderer 抽象类框架

<read_first>
- app/src/main/java/com/example/glearning/Day15Renderer.kt
- app/src/main/java/com/example/glearning/Day14Renderer.kt
- .planning/codebase/CONVENTIONS.md
- AGENTS.md
</read_first>

<acceptance_criteria>
- BaseFilterRenderer.kt 文件创建在 app/src/main/java/com/example/glearning/
- 类声明为 `abstract class BaseFilterRenderer : GLSurfaceView.Renderer`
- 包含 abstract 方法 `getFragmentShader(): String`
- 包含 abstract 方法 `setupUniforms(program: Int)`
- 遵循 Kotlin 代码风格（companion object 在前）
- 包含类级 KDoc 注释说明用途和核心概念
</acceptance_criteria>

<action>
创建抽象类 BaseFilterRenderer，实现 GLSurfaceView.Renderer 接口。

关键结构：
```kotlin
abstract class BaseFilterRenderer : GLSurfaceView.Renderer {
    companion object {
        // 统一顶点着色器代码（MVP + 纹理坐标）
        private const val VERTEX_SHADER_CODE = """..."""
        
        // 顶点数据格式常量
        private const val COORDS_PER_VERTEX = 2
        private const val UV_PER_VERTEX = 2
        private const val FLOAT_SIZE = 4
    }
    
    // 抽象方法 - 子类必须实现
    abstract fun getFragmentShader(): String
    abstract fun setupUniforms(program: Int)
    
    // ... 实现 GLSurfaceView.Renderer 的三个方法
}
```

顶点着色器必须包含：
- a_Position attribute (vec2)
- a_TextureCoord attribute (vec2)
- u_Matrix uniform (mat4)
- v_TextureCoord varying (vec2)
</action>

### Task 2: 实现渲染管线核心方法

<read_first>
- app/src/main/java/com/example/glearning/Day15Renderer.kt
- app/src/main/java/com/example/glearning/Day14Renderer.kt
- app/src/main/java/com/example/glearning/Day9Renderer.kt
</read_first>

<acceptance_criteria>
- onSurfaceCreated() 方法实现着色器加载、程序链接、uniform 句柄获取
- onSurfaceChanged() 方法实现视口设置和正交投影矩阵
- onDrawFrame() 方法实现绘制流程
- loadShader() 方法包含完整错误检查（参照 Day15）
- 包含 glDisableVertexAttribArray() 清理
- 所有 OpenGL 错误都有检查
</acceptance_criteria>

<action>
实现三个生命周期方法：

onSurfaceCreated:
1. 调用 loadShader() 加载顶点着色器和 getFragmentShader() 返回的片段着色器
2. 创建程序并链接
3. 获取 attribute 和 uniform 句柄
4. 调用 setupUniforms(program) 让子类设置特定 uniform
5. 初始化 startTime 用于动画

onSurfaceChanged:
1. GLES20.glViewport(0, 0, width, height)
2. 计算 aspectRatio = width.toFloat() / height.toFloat()
3. 使用 WORLD_HALF_SIZE = 100f 设置正交投影矩阵
4. 横屏：left/right = ±(aspectRatio * WORLD_HALF_SIZE)
5. 竖屏：bottom/top = ±(WORLD_HALF_SIZE / aspectRatio)

onDrawFrame:
1. glClearColor(0.0f, 0.0f, 0.0f, 1.0f) + glClear
2. 绑定纹理（根据 useCameraTexture 标志）
3. 设置 MVP 矩阵
4. 启用 attribute 数组
5. 调用 setupUniforms() 更新参数（仅当参数变化时）
6. glDrawElements 绘制
7. glDisableVertexAttribArray 清理
</action>

### Task 3: 实现双模式纹理支持

<read_first>
- app/src/main/java/com/example/glearning/Day15Renderer.kt
- app/src/main/java/com/example/glearning/Day14Renderer.kt
- app/src/main/java/com/example/glearning/CameraHelper.kt
</read_first>

<acceptance_criteria>
- 包含 useCameraTexture: Boolean 属性
- 包含 createProceduralTexture() 方法创建程序化纹理
- 包含 setupOESTexture() 方法设置 OES 外部纹理
- 包含 textureId: IntArray 和 oesTextureId: IntArray
- 包含 CameraHelper 实例
- 支持运行时切换纹理模式
</acceptance_criteria>

<action>
添加双模式纹理支持：

属性：
- var useCameraTexture = false（默认程序化纹理）
- private var textureId = IntArray(1)
- private var oesTextureId = IntArray(1)
- private var cameraHelper: CameraHelper? = null
- private var surfaceTexture: SurfaceTexture? = null

方法：
- createProceduralTexture(): 创建 256x256 渐变纹理（类似 Day14）
- setupOESTexture(): 创建 GLES11Ext.GL_TEXTURE_EXTERNAL_OES 纹理
- switchTextureMode(useCamera: Boolean): 切换模式

在 onSurfaceCreated 中：
- 如果 useCameraTexture 为 false，调用 createProceduralTexture()
- 如果 useCameraTexture 为 true，调用 setupOESTexture() 并初始化 CameraHelper

在片段着色器中：
- 程序化纹理模式使用 sampler2D
- 相机模式使用 samplerExternalOES（需要 #extension）
</action>

### Task 4: 实现参数缓存和更新机制

<read_first>
- app/src/main/java/com/example/glearning/Day14Renderer.kt
</read_first>

<acceptance_criteria>
- 包含参数缓存机制（记录上次值）
- 只在参数变化时调用 glUniform
- 包含 updateUniformIfNeeded(handle: Int, newValue: Float, oldValue: Float) 方法
- 减少不必要的 GL 调用
</acceptance_criteria>

<action>
实现参数缓存和更新机制：

添加方法：
```kotlin
protected fun updateUniformIfNeeded(
    handle: Int,
    newValue: Float,
    oldValue: Float
): Float {
    if (abs(newValue - oldValue) > 0.001f) {
        GLES20.glUniform1f(handle, newValue)
        return newValue
    }
    return oldValue
}
```

使用模式：
- 子类维护当前参数值（如 currentBrightness）
- 在 onDrawFrame 中调用 updateUniformIfNeeded()
- 只有返回值与旧值不同时才更新缓存

这确保了：
- 参数不变时不产生 GL 调用开销
- 参数变化时立即更新
- 使用 0.001f 阈值避免浮点误差
</action>

---

*Plan 01 created: 2026-05-11*
