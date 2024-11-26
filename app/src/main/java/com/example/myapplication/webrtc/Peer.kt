package com.example.myapplication.webrtc

import android.app.Activity
import android.app.Application
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.webrtc.AudioTrack
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.EglBase10.Context
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class Peer(
    private val context: Activity,
    private val observer: PeerConnection.Observer,
    private val videoCapturer: VideoCapturer
) {

    private val rootEglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val peerConnection: PeerConnection by lazy { createPeerConnection() }

    private var videoTrack: VideoTrack? = null
    private var audioTrack: AudioTrack? = null

    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }

    init {
        initPeerConnectionFactory(context.application)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        return PeerConnectionFactory.builder()
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(rootEglBase.eglBaseContext))
            .setVideoEncoderFactory(DefaultVideoEncoderFactory(rootEglBase.eglBaseContext, true, true))
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
            })
            .createPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnection(): PeerConnection {
        return peerConnectionFactory.createPeerConnection(getRTCConfig(), observer)!!
    }

    private fun getRTCConfig(): PeerConnection.RTCConfiguration {
        return PeerConnection.RTCConfiguration(getIceServers()).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_ONCE
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            iceTransportsType = PeerConnection.IceTransportsType.ALL
            enableCpuOveruseDetection = false
        }
    }

    private fun getIceServers(): List<IceServer> {
        // return your ICE server list here (like from signaling server or static)
        return listOf()
    }

    // Initialize the video capturer and add video track if needed
    fun addVideoTrack() {
        // Initialize the video capturer with the passed instance
        val surfaceTextureHelper = SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        videoCapturer.initialize(surfaceTextureHelper, context, localVideoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)

        videoTrack = peerConnectionFactory.createVideoTrack("videoTrack", localVideoSource)
        peerConnection.addTrack(videoTrack)
    }

    // Add audio track manually
    fun addAudioTrack() {
        val audioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        audioTrack = peerConnectionFactory.createAudioTrack("audioTrack", audioSource)
        peerConnection.addTrack(audioTrack)
    }

    suspend fun createOffer(): SessionDescription {
        return suspendCancellableCoroutine { continuation ->
            val constraints = MediaConstraints()
            peerConnection.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp != null) {
                        continuation.resume(sdp)
                    } else {
                        continuation.resumeWithException(NullPointerException("SDP is null"))
                    }
                }

                override fun onCreateFailure(error: String?) {
                    continuation.resumeWithException(Exception("SDP creation failed: $error"))
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(error: String?) {}
            }, constraints)
        }
    }

    // Add offer and set as remote description manually
    fun addOffer(offer: SessionDescription) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetSuccess() {
                // Handle successful set remote offer
            }
            override fun onSetFailure(error: String?) {}
        }, offer)
    }

    // Function to create answer
    suspend fun createAnswer(): SessionDescription {
        return suspendCancellableCoroutine { continuation ->
            val constraints = MediaConstraints()
            peerConnection.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    if (sdp != null) {
                        continuation.resume(sdp)
                    } else {
                        continuation.resumeWithException(NullPointerException("SDP is null"))
                    }
                }

                override fun onCreateFailure(error: String?) {
                    continuation.resumeWithException(Exception("SDP creation failed: $error"))
                }

                override fun onSetSuccess() {}
                override fun onSetFailure(error: String?) {}
            }, constraints)
        }
    }

    // Add answer and set as remote description
    fun addAnswer(answer: SessionDescription) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {}
            override fun onCreateFailure(error: String?) {}
            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, answer)
    }

    // Close the peer connection and stop the video capturer
    fun close() {
        peerConnection.close()
        peerConnectionFactory.dispose()
    }
}