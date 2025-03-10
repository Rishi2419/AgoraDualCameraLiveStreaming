package com.example.agoradualcamerastream

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.agora.rtc2.ClientRoleOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import io.agora.rtc2.video.VideoEncoderConfiguration

class LiveStreamActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHANNEL_NAME = "rs"
        const val EXTRA_IS_HOST = "isHost"
        private const val APP_ID = "2298ff7865d14062afec8e8cedd5daf5" // Replace with your Agora App ID
    }

    private lateinit var rtcEngine: RtcEngine
    private var isMuted = false
    private var isHost = false
    private var channelName = ""
    private var isFrontCamera = true
    private var usersVisible = false
    private var localUid = 0

    private val userList = ArrayList<User>()
    private lateinit var userAdapter: UserAdapter

    private lateinit var mainVideoContainer: FrameLayout
    private lateinit var secondaryVideoContainer: FrameLayout
    private lateinit var btnEndStream: ImageButton
    private lateinit var btnSwitchCamera: ImageButton
    private lateinit var btnMute: ImageButton
    private lateinit var btnUsers: ImageButton
    private lateinit var rvUsers: RecyclerView
    private lateinit var tvMainCameraLabel: TextView
    private lateinit var tvSecondaryCameraLabel: TextView
    private lateinit var signalingManager: SignalingManager

    private lateinit var dualCameraManager: DualCameraManager


    private val mRtcEventHandler = object : IRtcEngineEventHandler() {
        // Override methods to handle RTC events
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                localUid = uid
                if (isHost) {
                    setupLocalVideo()
                }
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                if (!isHost) {
                    setupRemoteVideo(uid)
                } else {
                    val user = User(uid, "User $uid")
                    if (!userList.contains(user)) {
                        userList.add(user)
                        userAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                userList.removeAll { it.uid == uid }
                userAdapter.notifyDataSetChanged()

                // If host is watching this user, remove the view
                if (secondaryVideoContainer.tag == uid.toString()) {
                    secondaryVideoContainer.removeAllViews()
                    secondaryVideoContainer.visibility = View.GONE
                }
            }
        }
//
//        override fun onClientRoleChanged(oldRole: Int, newRole: Int) {
//            runOnUiThread {
//                if (newRole == Constants.CLIENT_ROLE_BROADCASTER) {
//                    if (!isHost) {
//                        setupLocalVideo()
//                    }
//                } else {
//                    secondaryVideoContainer.removeAllViews()
//                    secondaryVideoContainer.visibility = View.GONE
//                }
//            }
//        }

        override fun onClientRoleChanged(oldRole: Int, newRole: Int, newRoleOptions: ClientRoleOptions?) {
            runOnUiThread {
                if (newRole == Constants.CLIENT_ROLE_BROADCASTER) {
                    if (!isHost) {
                        setupLocalVideo()
                    }
                } else {
                    secondaryVideoContainer.removeAllViews()
                    secondaryVideoContainer.visibility = View.GONE
                }
            }
        }


        override fun onStreamMessage(uid: Int, streamId: Int, data: ByteArray) {
            runOnUiThread {
                signalingManager.processReceivedMessage(data)
            }
        }

//        override fun onClientRoleChanged(oldRole: Int, newRole: Int, newRoleOptions: ClientRoleOptions?) {
//            runOnUiThread {
//                if (newRole == Constants.CLIENT_ROLE_BROADCASTER) {
//                    // User switched to broadcaster role
//                    if (!isHost) {
//                        setupLocalVideo()
//                    }
//                } else {
//                    // User switched to audience role
//                    secondaryVideoContainer.removeAllViews()
//                    secondaryVideoContainer.visibility = View.GONE
//                }
//            }
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_stream)

        channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: ""
        isHost = intent.getBooleanExtra(EXTRA_IS_HOST, false)

        mainVideoContainer = findViewById(R.id.mainVideoContainer)
        secondaryVideoContainer = findViewById(R.id.secondaryVideoContainer)
        btnEndStream = findViewById(R.id.btnEndStream)
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera)
        btnMute = findViewById(R.id.btnMute)
        btnUsers = findViewById(R.id.btnUsers)
        rvUsers = findViewById(R.id.rvUsers)
        tvMainCameraLabel = findViewById(R.id.tvMainCameraLabel)
        tvSecondaryCameraLabel = findViewById(R.id.tvSecondaryCameraLabel)

        // Initialize RtcEngine
        initializeEngine()

        // Setup user list recycler view
        setupUserList()

        // Setup UI components
        setupUI()

        // Join the channel
        joinChannel()

        setupSignaling()
    }

    private fun initializeEngine() {
        try {
            rtcEngine = RtcEngine.create(baseContext, APP_ID, mRtcEventHandler)

            // Enable video module
            rtcEngine.enableVideo()

            // Set video encoder configuration
            rtcEngine.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )

            // Set audio profile
            rtcEngine.setAudioProfile(
                Constants.AUDIO_PROFILE_MUSIC_HIGH_QUALITY,
                Constants.AUDIO_SCENARIO_GAME_STREAMING
            )
            dualCameraManager = DualCameraManager(this, rtcEngine)
        } catch (e: Exception) {
            e.printStackTrace()
            finish()
        }
    }

    private fun setupSignaling() {
        signalingManager = SignalingManager(rtcEngine, channelName, localUid)
        signalingManager.setSignalingListener(object : SignalingManager.SignalingListener {
            override fun onJoinRequest(fromUid: Int, userName: String) {
                runOnUiThread {
                    val user = User(fromUid, userName)
                    showJoinRequestDialog(user)
                }
            }

            override fun onJoinRequestAccepted(fromUid: Int) {
                runOnUiThread {
                    showToast("Your request to join has been accepted!")
                    rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
                }
            }

            override fun onJoinRequestRejected(fromUid: Int) {
                runOnUiThread {
                    showToast("Your request to join has been rejected")
                }
            }
        })
    }

    private fun setupUserList() {
        userAdapter = UserAdapter(userList) { user ->
            // Handle request to join as broadcaster
            if (isHost) {
                showJoinRequestDialog(user)
            } else {
                // Audience requests to join
                signalingManager.sendJoinRequest(user.uid)
                showToast("Request sent to host")
                // In a real app, you would send a request through signaling
            }
        }

        rvUsers.apply {
            layoutManager = LinearLayoutManager(this@LiveStreamActivity)
            adapter = userAdapter
        }
    }

    private fun setupUI() {
        // Initially hide secondary video for audience
//        if (!isHost) {
//            secondaryVideoContainer.visibility = View.GONE
//        }

        btnEndStream.setOnClickListener {
            leaveChannel()
        }

        btnSwitchCamera.setOnClickListener {
            rtcEngine.switchCamera()
            isFrontCamera = !isFrontCamera
            updateCameraLabels()
        }

        btnMute.setOnClickListener {
            isMuted = !isMuted
            rtcEngine.muteLocalAudioStream(isMuted)
            btnMute.setImageResource(
                if (isMuted) android.R.drawable.ic_lock_silent_mode
                else android.R.drawable.ic_lock_silent_mode_off
            )
        }

        btnUsers.setOnClickListener {
            usersVisible = !usersVisible
            rvUsers.visibility = if (usersVisible) View.VISIBLE else View.GONE
        }

        updateCameraLabels()
    }

    private fun updateCameraLabels() {
        if (isHost) {
            tvMainCameraLabel.text = if (isFrontCamera) "FRONT CAMERA" else "REAR CAMERA"
        }
    }

    private fun joinChannel() {
        // Set client role
        if (isHost) {
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
        } else {
            rtcEngine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE)
        }

        // Join the channel
        rtcEngine.joinChannel(null, channelName, null, 0)
        rtcEngine.joinChannel(null, channelName, null, 1)

        setupSignaling()
    }

//    private fun setupLocalVideo() {
//        // Create SurfaceView
//
//        val surfaceView = RtcEngine.CreateRendererView(baseContext)
//        surfaceView.setZOrderMediaOverlay(true)
//
//        // Add to the appropriate container based on role
//        if (isHost) {
//            mainVideoContainer.addView(surfaceView)
//        } else {
//            secondaryVideoContainer.addView(surfaceView)
//            secondaryVideoContainer.visibility = View.VISIBLE
//        }
//
//        // Setup local video
//        rtcEngine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
//        rtcEngine.startPreview()
//    }

    private fun setupLocalVideo() {
        // 🔹 Create two TextureViews for front and back cameras
        val frontTextureView = TextureView(this)
        val backTextureView = TextureView(this)

        // 🔹 Set Layout Params
        frontTextureView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        backTextureView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )

        // 🔹 Clear any existing views before adding new ones
        mainVideoContainer.removeAllViews()
        secondaryVideoContainer.removeAllViews()

        // 🔹 Add views to the UI
        mainVideoContainer.addView(backTextureView)
        secondaryVideoContainer.addView(frontTextureView)
        secondaryVideoContainer.visibility = View.VISIBLE

        // 🔹 Start dual-camera streaming
        dualCameraManager.startDualCamera(frontTextureView, backTextureView)

        // 🔹 Set up local video streams
        rtcEngine.setupLocalVideo(VideoCanvas(backTextureView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
        rtcEngine.setupLocalVideo(VideoCanvas(frontTextureView, VideoCanvas.RENDER_MODE_HIDDEN, 1))

        // 🔹 Start preview (VERY IMPORTANT!)
        rtcEngine.startPreview()
    }


//    private fun setupRemoteVideo(uid: Int) {
//        // Create SurfaceView
//        val surfaceView = RtcEngine.CreateRendererView(baseContext)
//
//        // Add to the appropriate container based on role
//        if (!isHost) {
//            // Audience watching host
//            mainVideoContainer.addView(surfaceView)
//        } else {
//            // Host watching audience
//            secondaryVideoContainer.removeAllViews()
//            secondaryVideoContainer.addView(surfaceView)
//            secondaryVideoContainer.visibility = View.VISIBLE
//            secondaryVideoContainer.tag = uid.toString()
//        }
//
//        // Setup remote video
//        rtcEngine.setupRemoteVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
//    }


//    private fun setupRemoteVideo(uid: Int) {
//        val remoteView = RtcEngine.CreateRendererView(this)
//        remoteView.setZOrderMediaOverlay(true)
//
//        mainVideoContainer.removeAllViews()
//        mainVideoContainer.addView(remoteView)
//
//        rtcEngine.setupRemoteVideo(VideoCanvas(remoteView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
//    }


    private fun setupRemoteVideo(uid: Int) {
        val remoteFrontTextureView = TextureView(this)
        val remoteBackTextureView = TextureView(this)

        remoteFrontTextureView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )
        remoteBackTextureView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        )

        mainVideoContainer.removeAllViews()
        secondaryVideoContainer.removeAllViews()

        mainVideoContainer.addView(remoteBackTextureView)
        secondaryVideoContainer.addView(remoteFrontTextureView)
        secondaryVideoContainer.visibility = View.VISIBLE

        // ✅ Use separate UIDs for front and back cameras
        rtcEngine.setupRemoteVideo(VideoCanvas(remoteFrontTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid))
        rtcEngine.setupRemoteVideo(VideoCanvas(remoteBackTextureView, VideoCanvas.RENDER_MODE_HIDDEN, uid + 1))

        rtcEngine.startPreview()
    }



    private fun showJoinRequestDialog(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_join_request, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val tvRequestMessage = dialogView.findViewById<TextView>(R.id.tvRequestMessage)
        tvRequestMessage.text = "User ${user.name} wants to join your stream"

        dialogView.findViewById<View>(R.id.btnAccept).setOnClickListener {
            signalingManager.acceptJoinRequest(user.uid)
            // In a real app, you would signal to the user that they can start broadcasting
            showToast("Request accepted")
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btnDecline).setOnClickListener {
            signalingManager.rejectJoinRequest(user.uid)
            showToast("Request declined")
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun leaveChannel() {
        rtcEngine.leaveChannel()
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        leaveChannel()
        dualCameraManager.release()
        RtcEngine.destroy()
    }

    data class User(val uid: Int, val name: String) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as User
            return uid == other.uid
        }

        override fun hashCode(): Int {
            return uid
        }
    }
}