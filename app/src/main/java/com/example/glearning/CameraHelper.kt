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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface

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
    private var cameraHandler: Handler = Handler(Looper.getMainLooper())
    
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
        if (!hasCameraPermission()) {
            Log.w(TAG, "没有相机权限，无法启动相机")
            return
        }
        
        if (previewSurface == null) {
            Log.w(TAG, "预览 Surface 未设置，无法启动相机")
            return
        }
        
        val cameraId = getBackCameraId()
        if (cameraId == null) {
            Log.e(TAG, "找不到后置摄像头")
            return
        }
        
        openCamera(cameraId)
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
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null
            
            cameraDevice?.close()
            cameraDevice = null
            
            isCameraOpened = false
            Log.d(TAG, "相机已停止")
        } catch (e: Exception) {
            Log.e(TAG, "停止相机失败: ${e.message}")
        }
    }
    
    /**
     * 检查相机权限
     */
    fun hasCameraPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * 获取后置摄像头 ID
     *
     * 遍历所有摄像头，找到第一个后置摄像头
     * 简化实现：不考虑多摄像头场景
     */
    private fun getBackCameraId(): String? {
        try {
            for (cameraId in cameraManager.cameraIdList) {
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_BACK) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取摄像头列表失败: ${e.message}")
        }
        return null
    }
    
    /**
     * 打开相机设备
     *
     * 使用 CameraManager.openCamera() 打开相机
     * CameraDevice.StateCallback 处理打开成功/失败事件
     */
    private fun openCamera(cameraId: String) {
        try {
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    Log.d(TAG, "相机设备已打开")
                    cameraDevice = camera
                    isCameraOpened = true
                    startPreview()
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    Log.w(TAG, "相机设备断开连接")
                    camera.close()
                    cameraDevice = null
                    isCameraOpened = false
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    Log.e(TAG, "相机设备打开失败: error=$error")
                    camera.close()
                    cameraDevice = null
                    isCameraOpened = false
                }
            }, cameraHandler)
        } catch (e: Exception) {
            Log.e(TAG, "打开相机失败: ${e.message}")
        }
    }
    
    /**
     * 创建预览 CaptureRequest
     *
     * 使用 TEMPLATE_PREVIEW 模板创建基础请求
     * 将 Surface 添加为预览目标
     */
    private fun createPreviewRequest(): CaptureRequest? {
        val device = cameraDevice ?: return null
        val surface = previewSurface ?: return null
        
        try {
            val builder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)
            return builder.build()
        } catch (e: Exception) {
            Log.e(TAG, "创建预览请求失败: ${e.message}")
            return null
        }
    }
    
    /**
     * 启动预览
     *
     * 流程：
     * 1. 创建 CaptureRequest
     * 2. 创建 CaptureSession
     * 3. 设置连续预览请求
     */
    private fun startPreview() {
        val device = cameraDevice ?: return
        val surface = previewSurface ?: return
        val request = createPreviewRequest() ?: return
        
        try {
            device.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        Log.d(TAG, "CaptureSession 已配置")
                        captureSession = session
                        try {
                            session.setRepeatingRequest(request, null, cameraHandler)
                            Log.d(TAG, "预览已启动")
                        } catch (e: Exception) {
                            Log.e(TAG, "设置预览请求失败: ${e.message}")
                        }
                    }
                    
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Log.e(TAG, "CaptureSession 配置失败")
                    }
                },
                cameraHandler
            )
        } catch (e: Exception) {
            Log.e(TAG, "创建 CaptureSession 失败: ${e.message}")
        }
    }
}