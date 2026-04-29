package com.example.glearning

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * MainActivity: 权限请求 + 生命周期管理
 *
 * 关键流程：
 * 1. onCreate: 检查权限 + 创建 GLSurfaceView + 创建 CameraHelper + 创建 Renderer
 * 2. onResume: glSurfaceView.onResume() + cameraHelper.startCamera()
 * 3. onPause: cameraHelper.stopCamera() + glSurfaceView.onPause()
 *
 * 数据传递方向：
 * - Renderer.onSurfaceCreated() 创建 SurfaceTexture → 将其 Surface 传给 CameraHelper
 * - CameraHelper.setPreviewSurface(surface) 接收 Surface → 用于相机预览
 */
class MainActivity : AppCompatActivity() {
    
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var cameraHelper: CameraHelper
    
    // 使用 Activity 结果 API 处理权限请求（推荐方式）
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
        
        // 创建 GLSurfaceView
        glSurfaceView = GLSurfaceView(this)
        glSurfaceView.setEGLContextClientVersion(2)
        
        // 创建 CameraHelper
        cameraHelper = CameraHelper(this)
        
        // 创建 Renderer（传入 CameraHelper 和 GLSurfaceView）
        // Renderer 在 onSurfaceCreated 中会创建 SurfaceTexture 并传给 CameraHelper
        // GLSurfaceView 用于在帧到达时触发渲染
        val renderer = Day15Renderer(cameraHelper, glSurfaceView)
        glSurfaceView.setRenderer(renderer)
        
        setContentView(glSurfaceView)
        
        // 检查相机权限
        checkCameraPermission()
    }
    
    /**
     * 检查相机权限
     *
     * 如果有权限，相机将在 onResume 时启动
     * 如果无权限，使用 Activity 结果 API 请求权限
     */
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            // 已有权限
        } else {
            // 使用 Activity 结果 API 请求权限（推荐方式）
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
        
        // 启动相机
        // 注意：此时 Renderer.onSurfaceCreated() 已执行
        // SurfaceTexture 已创建并传给 CameraHelper
        if (cameraHelper.hasCameraPermission()) {
            cameraHelper.startCamera()
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 停止相机（先停止相机，再暂停 GLSurfaceView）
        cameraHelper.stopCamera()
        glSurfaceView.onPause()
    }
}