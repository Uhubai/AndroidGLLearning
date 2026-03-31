# AGENTS.md

本项目是一个 Android OpenGL 学习项目。本文档为 AI 编码代理提供项目指南。

## 项目概述

- **项目类型**: Android OpenGL ES 学习项目
- **操作系统**: Windows 11
- **终端**: PowerShell 7
- **输出语言**: 简体中文

## 构建命令

### Android 项目命令

```powershell
# 构建项目
./gradlew assembleDebug

# 构建发布版本
./gradlew assembleRelease

# 清理构建
./gradlew clean

# 安装到设备
./gradlew installDebug

# 运行所有测试
./gradlew test

# 运行单个测试类
./gradlew test --tests "com.example.app.ClassName"

# 运行单个测试方法
./gradlew test --tests "com.example.app.ClassName.testMethodName"

# Android 仪器测试
./gradlew connectedAndroidTest

# Lint 检查
./gradlew lint

# 代码格式化 (如果使用 spotless)
./gradlew spotlessApply

# 依赖检查
./gradlew dependencies
```

## 代码风格指南

### 命名约定

- **类名**: PascalCase，如 `GLRenderer`, `ShaderProgram`
- **方法名**: camelCase，如 `onSurfaceCreated`, `drawFrame`
- **常量**: UPPER_SNAKE_CASE，如 `VERTEX_SHADER_CODE`
- **资源文件**: snake_case，如 `activity_main.xml`
- **包名**: 全小写，如 `com.example.glearning`

### 文件组织

```
app/
├── src/main/
│   ├── java/com/example/glearning/
│   │   ├── renderer/          # OpenGL 渲染器
│   │   ├── shader/            # 着色器相关
│   │   ├── model/             # 数据模型
│   │   ├── util/              # 工具类
│   │   └── MainActivity.kt    # 主活动
│   ├── res/
│   │   ├── layout/            # 布局文件
│   │   ├── values/            # 字符串、颜色等
│   │   └── raw/               # 着色器文件 (.vert, .frag)
│   └── AndroidManifest.xml
└── build.gradle.kts
```

### Kotlin 代码风格

```kotlin
// 导入顺序：标准库 -> 第三方库 -> 项目内部
import android.opengl.GLES20
import java.nio.ByteBuffer
import com.example.glearning.util.GLUtils

// 类声明
class TriangleRenderer : GLSurfaceView.Renderer {
    
    companion object {
        private const val TAG = "TriangleRenderer"
        private const val COORDS_PER_VERTEX = 3
    }
    
    // 属性在 init 之前声明
    private var program: Int = 0
    private var positionHandle: Int = 0
    
    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        initializeShaders()
    }
    
    private fun initializeShaders() {
        // 实现
    }
}
```

### OpenGL ES 特定约定

- 着色器代码放在 `res/raw/` 目录
- 使用工具类封装 OpenGL 操作
- 检查所有 GL 操作的错误

```kotlin
object GLUtils {
    fun loadShader(type: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }
    
    fun checkGLError(tag: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(tag, "OpenGL Error: $error")
        }
    }
}
```

### 错误处理

- 使用 `Result<T>` 进行可能失败的操作
- OpenGL 错误需要立即检查
- 日志使用 `Log` 类，标签使用常量

```kotlin
fun createProgram(vertexShader: Int, fragmentShader: Int): Result<Int> {
    val program = GLES20.glCreateProgram()
    if (program == 0) {
        return Result.failure(GLException("Failed to create program"))
    }
    GLES20.glAttachShader(program, vertexShader)
    GLES20.glAttachShader(program, fragmentShader)
    GLES20.glLinkProgram(program)
    return Result.success(program)
}
```

### 注释规范

- 使用 KDoc 格式编写文档注释
- 复杂的 OpenGL 操作添加行内注释
- 重要的数学运算解释原理

```kotlin
/**
 * 创建正交投影矩阵
 * @param width 视口宽度
 * @param height 视口高度
 * @return 4x4 投影矩阵
 */
fun createOrthographicMatrix(width: Float, height: Float): FloatArray {
    // 正交投影矩阵计算
}
```

## Git 提交规范

```
<type>(<scope>): <subject>

type:
- feat: 新功能
- fix: 修复
- docs: 文档
- style: 格式
- refactor: 重构
- test: 测试
- chore: 构建/工具
```

## 项目特定规则

1. 所有模型输出使用简体中文
2. 终端命令使用 PowerShell 7 语法
3. 着色器文件使用 `.vert` 和 `.frag` 扩展名
4. OpenGL ES 版本: 2.0 或 3.0（根据设备兼容性）
5. 使用 Kotlin 而非 Java 编写代码