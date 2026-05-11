/**
 * MainActivity: 渲染器选择 + 生命周期管理
 *
 * 支持选择任意 Day 的渲染器进行复习：
 * - Day 1-14：普通渲染器，直接选择使用
 * - Day 15：相机渲染器，需要相机权限和 CameraHelper
 *
 * 点击右上角"切换"按钮可选择不同渲染器
 */
package com.example.glearning

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    
    private var glSurfaceView: GLSurfaceView? = null
    private var glContainer: FrameLayout? = null
    private var cameraHelper: CameraHelper? = null
    private var currentDay: Int = 14
    
    private val cameraPermissionLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "相机权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "相机权限被拒绝，无法显示预览", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 使用布局文件
        setContentView(R.layout.activity_main)
        
        // 获取容器和按钮
        glContainer = findViewById(R.id.glContainer)
        val btnSwitch = findViewById<Button>(R.id.btnSwitchRenderer)
        
        // 设置按钮点击事件
        btnSwitch.setOnClickListener {
            showRendererSelector()
        }
        
        // 初始化默认渲染器
        setupRenderer(currentDay)
        
        // 启动时显示选择菜单
        showRendererSelector()
    }
    
    private fun showRendererSelector() {
        val days = arrayOf(
            "Day 1: 纯色背景",
            "Day 2: 三角形",
            "Day 3: VBO + 索引缓冲",
            "Day 4: 正交投影",
            "Day 5: 平移动画",
            "Day 6-7: 五角星动画",
            "Day 8: 旋转矩阵",
            "Day 9: MVP 矩阵组合",
            "Day 10: 纹理基础",
            "Day 11: 纹理变换",
            "Day 12: 纹理混合",
            "Day 13: 复习 - 变换",
            "Day 14: 复习 - 综合",
            "Day 15: 相机预览",
            "Day 16: 亮度/对比度/饱和度",
            "Day 17: 灰度/反色/Sepia",
            "Day 18: 色调/色温",
            "Day 19: 滤镜管理器",
            "Day 20: OpenGL UI 控件",
            "Day 21: 扩展 UI 控件"
        )
        
        AlertDialog.Builder(this)
            .setTitle("选择渲染器 (当前: Day $currentDay)")
            .setItems(days) { _, which ->
                val newDay = which + 1
                if (newDay != currentDay) {
                    currentDay = newDay
                    setupRenderer(currentDay)
                    Toast.makeText(this, "已切换到 Day $currentDay", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("关闭", null)
            .show()
    }
    
    private fun setupRenderer(day: Int) {
        // 清理旧的相机资源
        cameraHelper?.stopCamera()
        cameraHelper = null
        
        // 暂停并移除旧的 GLSurfaceView
        glSurfaceView?.onPause()
        glContainer?.removeAllViews()
        
        // 创建新的 GLSurfaceView
        val newGlSurfaceView = GLSurfaceView(this)
        newGlSurfaceView.setEGLContextClientVersion(2)
        
        // 创建对应的渲染器
        if (day == 15) {
            val helper = CameraHelper(this)
            cameraHelper = helper
            checkCameraPermission()
            newGlSurfaceView.setRenderer(Day15Renderer(helper, newGlSurfaceView))
            newGlSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        } else {
            newGlSurfaceView.setRenderer(createRenderer(day, newGlSurfaceView))
            newGlSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        
        // 添加到容器
        glContainer?.addView(newGlSurfaceView)
        glSurfaceView = newGlSurfaceView
        
        // 启动渲染
        newGlSurfaceView.onResume()
        
        // 启动相机（如果是 Day 15）
        if (day == 15 && cameraHelper?.hasCameraPermission() == true) {
            cameraHelper?.startCamera()
        }
    }
    
    private fun createRenderer(day: Int, glSurfaceView: GLSurfaceView): GLSurfaceView.Renderer {
        return when (day) {
            1 -> Day1Renderer()
            2 -> Day2Renderer()
            3 -> Day3Renderer()
            4 -> Day4Renderer()
            5 -> Day5Renderer()
            6, 7 -> Day7Renderer()
            8 -> Day8Renderer()
            9 -> Day9Renderer()
            10 -> Day10Renderer()
            11 -> Day11Renderer()
            12 -> Day12Renderer()
            13 -> Day13Renderer()
            14 -> Day14Renderer()
            15 -> Day15Renderer(cameraHelper ?: CameraHelper(this), glSurfaceView)
            16 -> Day16Renderer()
            17 -> Day17Renderer()
            18 -> Day18Renderer()
            19 -> Day19Renderer()
            20 -> Day20Renderer()
            21 -> Day21Renderer()
            else -> Day14Renderer()
        }
    }
    
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
        if (currentDay == 15 && cameraHelper?.hasCameraPermission() == true) {
            cameraHelper?.startCamera()
        }
    }
    
    override fun onPause() {
        super.onPause()
        if (currentDay == 15) {
            cameraHelper?.stopCamera()
        }
        glSurfaceView?.onPause()
    }
}