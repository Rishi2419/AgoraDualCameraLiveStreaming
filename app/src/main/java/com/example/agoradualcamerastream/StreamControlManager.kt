package com.example.agoradualcamerastream

import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat

class StreamControlsManager(
    private val endStreamButton: ImageButton,
    private val switchCameraButton: ImageButton,
    private val muteButton: ImageButton,
    private val usersButton: ImageButton,
    private val usersListView: View
) {
    private var isMuted = false
    private var isUserListVisible = false

    fun setup(
        onEndStream: () -> Unit,
        onSwitchCamera: () -> Unit,
        onMuteToggle: (Boolean) -> Unit,
        onUsersToggle: (Boolean) -> Unit
    ) {
        // Setup end stream button
        endStreamButton.setOnClickListener {
            onEndStream()
        }

        // Setup switch camera button
        switchCameraButton.setOnClickListener {
            onSwitchCamera()
        }

        // Setup mute button
        muteButton.setOnClickListener {
            isMuted = !isMuted
            updateMuteButtonState()
            onMuteToggle(isMuted)
        }

        // Setup users button
        usersButton.setOnClickListener {
            isUserListVisible = !isUserListVisible
            updateUsersListVisibility()
            onUsersToggle(isUserListVisible)
        }
    }

    private fun updateMuteButtonState() {
        muteButton.setImageResource(
            if (isMuted) android.R.drawable.ic_lock_silent_mode
            else android.R.drawable.ic_lock_silent_mode_off
        )
    }

    private fun updateUsersListVisibility() {
        usersListView.visibility = if (isUserListVisible) View.VISIBLE else View.GONE
    }

    fun setMuted(muted: Boolean) {
        if (isMuted != muted) {
            isMuted = muted
            updateMuteButtonState()
        }
    }

    fun setUserListVisible(visible: Boolean) {
        if (isUserListVisible != visible) {
            isUserListVisible = visible
            updateUsersListVisibility()
        }
    }
}