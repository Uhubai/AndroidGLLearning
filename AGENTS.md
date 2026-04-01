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

### 注释规范（重要规则）

**所有生成的代码必须包含详细注释**。注释是代码可读性和学习价值的关键组成部分。

#### 注释原则

1. **每个类必须添加类级文档注释**：说明类的用途、核心概念、绘制内容
2. **每个公共方法必须添加文档注释**：说明执行时机、主要任务、参数含义、返回值
3. **关键代码块必须添加行内注释**：解释"为什么这样做"而非"做了什么"
4. **复杂的数据结构必须添加说明**：解释数据布局、格式、含义
5. **OpenGL/GPU 相关概念必须注释**：这些概念对初学者不直观

#### 注释层级

```kotlin
/**
 * 类级注释：说明整体目的、核心概念、学习重点
 *
 * 本类演示：
 * 1. 概念A - 解释
 * 2. 概念B - 解释
 * 3. 概念C - 解释
 *
 * 绘制内容：具体图形和效果描述
 */
class ExampleRenderer : GLSurfaceView.Renderer {
    
    companion object {
        // 常量注释：说明用途和含义
        private const val COORDS_PER_VERTEX = 2
        
        /**
         * 着色器代码注释：说明关键点和工作流程
         *
         * 关键变量：
         * - uniform: 全局共享数据
         * - attribute: 每顶点数据
         * - varying: 传递给片段着色器的数据
         */
        private const val SHADER_CODE = """
            // 着色器内部简要注释
        """
        
        /**
         * 数据结构注释：说明数据格式和布局
         *
         * 数据格式：交错存储
         * [x, y, r, g, b] × n 个顶点
         *
         * 顶点布局图示：
         *   顶点0 ──── 顶点1
         *   │          │
         *   顶点2 ──── 顶点3
         */
        private val VERTEX_DATA = floatArrayOf(...)
    }
    
    // 属性注释：说明用途和初始化时机
    private var program: Int = 0
    
    /**
     * 方法文档注释：完整说明
     *
     * 执行时机：何时调用
     * 主要任务：
     * 1. 任务A
     * 2. 任务B
     * 3. 任务C
     */
    override fun onSurfaceCreated(...) {
        // 步骤注释：分步骤解释流程
        // 步骤1：说明做什么和为什么
        
        // 行内注释：解释关键操作
        GLES20.glClearColor(...)
        
        // 参数说明：解释参数含义和范围
        // 参数范围：0.0f ~ 1.0f 表示颜色强度
    }
}
```

#### 注释内容要求

| 注释位置 | 必须包含的内容 |
|---------|--------------|
| **类级注释** | 类的用途、核心概念列表、绘制内容、学习重点 |
| **常量注释** | 常量的用途、含义、为什么使用这个值 |
| **着色器注释** | 变量类型说明、工作流程、关键点 |
| **数据结构注释** | 数据格式、布局图示、计算公式 |
| **属性注释** | 变量用途、初始化时机、数据含义 |
| **方法注释** | 执行时机、主要任务步骤、参数含义、返回值 |
| **关键代码注释** | 操作原理、为什么这样做、注意事项 |

#### OpenGL 特定注释要求

对于 OpenGL 相关代码，必须注释以下内容：

1. **着色器概念**：uniform vs attribute vs varying 的区别
2. **缓冲区操作**：为什么用直接内存、字节序的重要性
3. **矩阵变换**：变换顺序、各矩阵的作用
4. **绘制参数**：每个参数的含义和取值范围
5. **动画原理**：时间计算、三角函数的作用

#### 注释示例

```kotlin
/**
 * 创建正交投影矩阵
 *
 * 正交投影特点：
 * - 无透视效果，物体大小不随距离变化
 * - 适合 2D 图形和 UI 渲染
 * - 将世界坐标映射到 [-1, 1] 的标准化设备坐标
 *
 * @param width 视口宽度（像素）
 * @param height 视口高度（像素）
 * @return 4×4 投影矩阵（16 个浮点数）
 */
fun createOrthographicMatrix(width: Float, height: Float): FloatArray {
    // 计算宽高比，用于保持图形比例不变形
    val aspectRatio = width / height
    
    // 正交投影矩阵计算公式：
    // 左右范围 = ±aspectRatio（扩展以保持比例）
    // 上下范围 = ±1（固定）
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