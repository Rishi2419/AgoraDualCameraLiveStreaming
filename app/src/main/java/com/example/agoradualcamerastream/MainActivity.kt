package com.example.agoradualcamerastream
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var etChannelName: EditText
    private lateinit var btnJoinAsHost: Button
    private lateinit var btnJoinAsAudience: Button

    private val PERMISSION_REQUEST_ID = 22

    // Required permissions
    private val REQUESTED_PERMISSIONS = arrayOf(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etChannelName = findViewById(R.id.etChannelName)
        btnJoinAsHost = findViewById(R.id.btnJoinAsHost)
        btnJoinAsAudience = findViewById(R.id.btnJoinAsAudience)

        // Check if permissions are granted
        if (!checkPermissions()) {
            requestPermissions()
        }

        btnJoinAsHost.setOnClickListener {
            joinChannel(isHost = true)
        }

        btnJoinAsAudience.setOnClickListener {
            joinChannel(isHost = false)
        }
    }

    private fun joinChannel(isHost: Boolean) {
        val channelName = etChannelName.text.toString().trim()

        if (channelName.isEmpty()) {
            Toast.makeText(this, "Please enter a channel name", Toast.LENGTH_SHORT).show()
            return
        }

        if (!checkPermissions()) {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, LiveStreamActivity::class.java).apply {
            putExtra(LiveStreamActivity.EXTRA_CHANNEL_NAME, channelName)
            putExtra(LiveStreamActivity.EXTRA_IS_HOST, isHost)
        }
        startActivity(intent)
    }

    private fun checkPermissions(): Boolean {
        return REQUESTED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            REQUESTED_PERMISSIONS,
            PERMISSION_REQUEST_ID
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_ID) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted. The app may not work properly.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}