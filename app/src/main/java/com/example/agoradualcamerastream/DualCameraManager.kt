package com.example.agoradualcamerastream

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import io.agora.rtc2.*
import io.agora.rtc2.video.*
import java.util.concurrent.Executors

class DualCameraManager(
    private val context: Context,
    private val rtcEngine: RtcEngine
) {
    private var frontCameraId: String? = null
    private var backCameraId: String? = null
    private var cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var frontCameraSession: CameraCaptureSession? = null
    private var backCameraSession: CameraCaptureSession? = null
    private var frontCameraDevice: CameraDevice? = null
    private var backCameraDevice: CameraDevice? = null

    private var frontTextureView: TextureView? = null
    private var backTextureView: TextureView? = null

    private val backgroundThread = HandlerThread("CameraBackgroundThread").apply { start() }
    private val backgroundHandler = Handler(backgroundThread.looper)

    init {
        setupCameraIds()
    }

    private fun setupCameraIds() {
        for (cameraId in cameraManager.cameraIdList) {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                frontCameraId = cameraId
            } else if (lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                backCameraId = cameraId
            }
        }
    }

    fun startDualCamera(frontView: TextureView, backView: TextureView) {
        frontTextureView = frontView
        backTextureView = backView

        frontTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera(frontCameraId, surface, true)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        backTextureView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera(backCameraId, surface, false)
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun openCamera(cameraId: String?, surfaceTexture: SurfaceTexture, isFront: Boolean) {
        cameraId ?: return
        try {
            val surface = Surface(surfaceTexture)
            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    if (isFront) {
                        frontCameraDevice = camera
                    } else {
                        backCameraDevice = camera
                    }
                    createCameraSession(camera, surface, isFront)
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                }
            }, backgroundHandler) // 🔹 FIXED: Using backgroundHandler instead of non-existent executor.handler
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun createCameraSession(camera: CameraDevice, surface: Surface, isFront: Boolean) {
        try {
            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        val requestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        requestBuilder.addTarget(surface)
                        session.setRepeatingRequest(requestBuilder.build(), null, backgroundHandler)

                        if (isFront) {
                            frontCameraSession = session
                        } else {
                            backCameraSession = session
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                backgroundHandler
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        frontCameraSession?.close()
        backCameraSession?.close()
        frontCameraDevice?.close()
        backCameraDevice?.close()
        frontCameraSession = null
        backCameraSession = null
        frontCameraDevice = null
        backCameraDevice = null

        // Stop background thread
        backgroundThread.quitSafely()
    }
}
