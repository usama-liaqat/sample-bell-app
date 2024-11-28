package com.example.myapplication.webrtc

import android.app.Activity
import com.example.myapplication.Config
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.HardwareVideoDecoderFactory
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

class PeerFactory(
    private val activity: Activity,
    private val rootEglBase:EglBase = EglBase.create()
) {


    private val peerConnectionFactory: PeerConnectionFactory by lazy { buildPeerConnectionFactory() }

    private val iceServers: MutableList<PeerConnection.IceServer> = mutableListOf()

    init {
        initPeerConnectionFactory()
    }

    fun iceServersList(): List<PeerConnection.IceServer> = iceServers


    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        val videoEncoderFactory = HardwareVideoEncoderFactory(
            rootEglBase.eglBaseContext, false, true
        )
        val videoDecoderFactory = HardwareVideoDecoderFactory(rootEglBase.eglBaseContext)
        return PeerConnectionFactory.builder().setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(activity.application)
            .setEnableInternalTracer(true).createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }


    fun createPeerConnection(
        rtcConfig: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer,
    ): PeerConnection {
        return peerConnectionFactory.createPeerConnection(rtcConfig, observer)!!
    }


    fun getRTCConfig(iceServers: List<PeerConnection.IceServer> = listOf()): PeerConnection.RTCConfiguration {
        return PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            enableCpuOveruseDetection = false
        }
    }


    fun getIceServers(identity:String): List<PeerConnection.IceServer> {
        if (iceServers.isNotEmpty()) {
            return iceServers
        }
        val client = OkHttpClient()
        val mediaType = "application/json".toMediaType()
        val requestBody = """
            {
                "identity": "$identity",
                "key": "${Config.TURN_API_KEY}"
            }
        """.trimIndent().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(Config.TURN_API_SERVER_URI)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()

        val iceCandidate = response.body?.string() ?: "[]"
        val ice = JSONArray(iceCandidate)

        iceServers.clear() // Clear previous servers to avoid duplicates
        for (i in 0 until ice.length()) {
            val data = ice.getJSONObject(i)
            val username = data.optString("username", "")
            val password = data.optString("credential", "")
            val url = data.optString("url", "")

            if (url.isNotEmpty()) {
                iceServers.add(
                    PeerConnection.IceServer.builder(url)
                        .setUsername(username)
                        .setPassword(password)
                        .createIceServer()
                )
            }
        }

        return iceServers
    }

    fun createVideoSource(isScreencast: Boolean): VideoSource {
         return peerConnectionFactory.createVideoSource(isScreencast)
    }

    fun createAudioSource(constraints: MediaConstraints): AudioSource {
         return peerConnectionFactory.createAudioSource(constraints)
    }

    fun createAudioTrack(id: String,  source: AudioSource): AudioTrack {
        return peerConnectionFactory.createAudioTrack(id, source)
    }

     fun createVideoTrack( id:String,  source:VideoSource):VideoTrack {
         return peerConnectionFactory.createVideoTrack(id, source)
     }

    fun createLocalMediaStream(label:String):MediaStream {
        return peerConnectionFactory.createLocalMediaStream(label)
    }
}