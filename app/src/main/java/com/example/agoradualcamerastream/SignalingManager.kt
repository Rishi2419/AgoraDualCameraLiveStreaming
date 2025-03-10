package com.example.agoradualcamerastream

import io.agora.rtc2.RtcEngine
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class SignalingManager(
    private val rtcEngine: RtcEngine,
    private val channelName: String,
    private val localUid: Int
) {
    interface SignalingListener {
        fun onJoinRequest(fromUid: Int, userName: String)
        fun onJoinRequestAccepted(fromUid: Int)
        fun onJoinRequestRejected(fromUid: Int)
    }

    private var signalingListener: SignalingListener? = null
    private val pendingRequests = ConcurrentHashMap<Int, Boolean>()

    fun setSignalingListener(listener: SignalingListener) {
        this.signalingListener = listener
    }

    fun sendJoinRequest(toUid: Int) {
        val message = JSONObject().apply {
            put("type", "joinRequest")
            put("fromUid", localUid)
            put("toUid", toUid)
            put("channelName", channelName)
        }

        rtcEngine.sendStreamMessage(
            createDataStreamId(),
            message.toString().toByteArray()
        )

        pendingRequests[toUid] = true
    }

    fun acceptJoinRequest(toUid: Int) {
        val message = JSONObject().apply {
            put("type", "joinRequestAccepted")
            put("fromUid", localUid)
            put("toUid", toUid)
            put("channelName", channelName)
        }

        rtcEngine.sendStreamMessage(
            createDataStreamId(),
            message.toString().toByteArray()
        )
    }

    fun rejectJoinRequest(toUid: Int) {
        val message = JSONObject().apply {
            put("type", "joinRequestRejected")
            put("fromUid", localUid)
            put("toUid", toUid)
            put("channelName", channelName)
        }

        rtcEngine.sendStreamMessage(
            createDataStreamId(),
            message.toString().toByteArray()
        )
    }

    fun processReceivedMessage(data: ByteArray) {
        try {
            val message = JSONObject(String(data))
            val type = message.getString("type")
            val fromUid = message.getInt("fromUid")
            val toUid = message.getInt("toUid")

            // Only process messages addressed to this user
            if (toUid != localUid) return

            when (type) {
                "joinRequest" -> {
                    val userName = "User $fromUid"
                    signalingListener?.onJoinRequest(fromUid, userName)
                }
                "joinRequestAccepted" -> {
                    pendingRequests.remove(fromUid)
                    signalingListener?.onJoinRequestAccepted(fromUid)
                }
                "joinRequestRejected" -> {
                    pendingRequests.remove(fromUid)
                    signalingListener?.onJoinRequestRejected(fromUid)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createDataStreamId(): Int {
        // Create a data stream for sending control messages
        return rtcEngine.createDataStream(true, true)
    }
}