package com.example.agoradualcamerastream

import android.content.Context
import android.view.TextureView
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas

class DualCameraManager(
    private val context: Context,
    private val rtcEngine: RtcEngine
) {
    private var primaryCameraView: TextureView? = null
    private var secondaryCameraView: TextureView? = null
    private var isFrontCameraPrimary = true

    fun setupDualCamera(primaryView: TextureView, secondaryView: TextureView) {
        primaryCameraView = primaryView
        secondaryCameraView = secondaryView

        // Setup primary camera (by default, front camera)
        val primarySurfaceView = RtcEngine.CreateRendererView(context)
        primaryView.tag = primarySurfaceView

        // Setup secondary camera
        val secondarySurfaceView = RtcEngine.CreateRendererView(context)
        secondaryView.tag = secondarySurfaceView

        // Initialize camera views
        updateCameraViews()
    }

    fun switchPrimaryCamera() {
        isFrontCameraPrimary = !isFrontCameraPrimary
        updateCameraViews()
        rtcEngine.switchCamera()
    }

    private fun updateCameraViews() {
        val primarySurfaceView = primaryCameraView?.tag as? TextureView ?: return
        val secondarySurfaceView = secondaryCameraView?.tag as? TextureView ?: return

        // Update camera configurations based on which is primary
        if (isFrontCameraPrimary) {
            // Front camera is primary
            rtcEngine.setupLocalVideo(VideoCanvas(primarySurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            // Secondary camera could be a different feed or remote user
        } else {
            // Rear camera is primary
            rtcEngine.setupLocalVideo(VideoCanvas(primarySurfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
            // Secondary camera could be a different feed or remote user
        }
    }

    fun release() {
        primaryCameraView = null
        secondaryCameraView = null
    }
}