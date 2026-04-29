/**
 * CameraHelper: Camera2 预览封装类
 *
 * 职责：
 * - 接收来自 Renderer 的 Surface 作为相机预览目标
 * - 管理 CameraDevice 和 CaptureSession
 * - 启动和停止相机预览
 *
 * 重要约束：
 * - 不创建 SurfaceTexture，而是接收 Surface
 * - SurfaceTexture 由 Renderer 在 GL 线程中创建
 * - 数据传递方向：Renderer → CameraHelper
 */
package com.example.glearning

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.view.Surface
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "CameraHelper"
        private const val DEFAULT_PREVIEW_WIDTH = 640
        private const val DEFAULT_PREVIEW_HEIGHT = 480
    }
    
    private var cameraManager: CameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var previewSurface: Surface? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var isCameraOpened: Boolean = false
    
    /**
     * 设置预览 Surface
     *
     * 由 Renderer 调用，将 SurfaceTexture 的 Surface 传入
     * 相机预览将输出到这个 Surface
     *
     * @param surface 从 SurfaceTexture 获取的 Surface
     */
    fun setPreviewSurface(surface: Surface) {
        this.previewSurface = surface
    }
    
    /**
     * 启动相机预览
     *
     * 流程：
     * 1. 检查权限
     * 2. 打开相机设备
     * 3. 创建预览请求
     * 4. 启动 CaptureSession
     */
    fun startCamera() {
        // TODO: 实现相机启动
    }
    
    /**
     * 停止相机预览
     *
     * 流程：
     * 1. 关闭 CaptureSession
     * 2. 关闭 CameraDevice
     * 3. 清理资源
     */
    fun stopCamera() {
        // TODO: 实现相机停止
    }
    
    /**
     * 检查相机权限
     */
    fun hasCameraPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}