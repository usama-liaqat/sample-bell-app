package com.example.myapplication.webrtc

import android.app.Activity
import android.app.Application
import android.util.Log
import com.example.myapplication.VideoItem
import com.example.myapplication.VideoViewAdapter
import com.example.myapplication.exchange.WHIPExchange
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.HardwareVideoDecoderFactory
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack


class WHIPPeer(
    private val activity: Activity,
    private val videoCapturer: VideoCapturer,
    private val exchange: WHIPExchange,
    private val videoViewAdapter: VideoViewAdapter,

    ) {
    private val TAG = "WHIPPeer"

    private val rootEglBase: EglBase = EglBase.create()
    private val peerConnectionFactory: PeerConnectionFactory by lazy { buildPeerConnectionFactory() }
    private val peerConnection: PeerConnection by lazy { createPeerConnection() }

    private var localAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null

    private val localVideoSource by lazy { peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy { peerConnectionFactory.createAudioSource(MediaConstraints()) }
    private val pendingCandidates = arrayListOf<IceCandidate>()


    init {
        initPeerConnectionFactory(activity.application)
    }

    private fun buildPeerConnectionFactory(): PeerConnectionFactory {
        val videoEncoderFactory = HardwareVideoEncoderFactory(
            rootEglBase.eglBaseContext, false, true
        )
        val supportedCodecs = videoEncoderFactory.supportedCodecs
        supportedCodecs.forEach { codec ->
            Log.d("SupportedCodec", "Codec Name: ${codec.name}")
            codec.params.forEach { (key, value) ->
                Log.d("SupportedCodec", "  Param: $key = $value")
            }
        }
        val videoDecoderFactory = HardwareVideoDecoderFactory(rootEglBase.eglBaseContext)
        return PeerConnectionFactory.builder().setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = true
            }).createPeerConnectionFactory()
    }

    private fun initPeerConnectionFactory(context: Application) {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true).createInitializationOptions()
        PeerConnectionFactory.initialize(options)
    }

    private fun createPeerConnection(): PeerConnection {
        return peerConnectionFactory.createPeerConnection(getRTCConfig(),
            object : PeerConnectionObserver() {
                override fun onIceCandidate(iceCandidate: IceCandidate?) {
                    if (iceCandidate !== null) {
                        addIceCandidate(iceCandidate)
                    }
                }
            })!!
    }

    fun initPeerConnection(): PeerConnection {
        peerConnectionFactory.createAudioSource(MediaConstraints())
        localAudioTrack =
            peerConnectionFactory.createAudioTrack(LOCAL_TRACK_ID + "_audio", localAudioSource)

        localVideoTrack = peerConnectionFactory.createVideoTrack(LOCAL_TRACK_ID, localVideoSource)
        val localStream = peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)

        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)

        localVideoTrack?.setEnabled(true)
        localAudioTrack?.setEnabled(true)

        peerConnection.addTrack(localVideoTrack, listOf(LOCAL_STREAM_ID))
        peerConnection.addTrack(localAudioTrack, listOf(LOCAL_STREAM_ID))

        return peerConnection
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

    fun connect() {
        createOffer()
    }

    fun startCapture() {
        val surfaceTextureHelper =
            SurfaceTextureHelper.create(Thread.currentThread().name, rootEglBase.eglBaseContext)
        videoCapturer.initialize(surfaceTextureHelper, activity, localVideoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)
    }

    private fun createOffer() {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "false"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "false"))
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                if (sdp != null) {
                    Log.e(TAG, "SDP Offer : ${sdp.description}")

                    addOffer(sdp)
                }
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "SDP Offer creation failed: $error")

            }

            override fun onSetSuccess() {}
            override fun onSetFailure(error: String?) {}
        }, constraints)
    }

    // Add offer and set as remote description manually
    fun addOffer(offer: SessionDescription) {
        peerConnection.setLocalDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d(TAG, "Local Description created")
            }

            override fun onCreateFailure(error: String?) {
                Log.d(TAG, "Local Description create failure ${error}")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "Local Description success")
                exchange.sendOffer(offer = offer, onSuccess = { response ->
                    Log.e(TAG, "SDP Offer Send Success")
                    val answer = SessionDescription(SessionDescription.Type.ANSWER, response)
                    addAnswer(answer)
                }, onError = { error ->
                    Log.e(TAG, "SDP Offer Send failed: $error")
                })
            }

            override fun onSetFailure(error: String?) {
                Log.d(TAG, "Local Description set failure ${error}")
            }
        }, offer)
    }

    // Add answer and set as remote description
    fun addAnswer(answer: SessionDescription) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.d(TAG, "Remote Description created")
            }

            override fun onCreateFailure(error: String?) {
                Log.d(TAG, "Remote Description create failure ${error}")
            }

            override fun onSetSuccess() {
                Log.d(TAG, "Remote Description success")
                pendingCandidates.forEach { candidate ->
                    sendIceCandidate(candidate)
                }
                pendingCandidates.clear()
            }

            override fun onSetFailure(error: String?) {
                Log.d(TAG, "Remote Description set failure ${error}")
            }
        }, answer)
    }

    private fun sendIceCandidate(iceCandidate: IceCandidate) {
        exchange.sendLocalCandidates(candidate = iceCandidate.sdp!!, onSuccess = {
            Log.e(TAG, "Ice Candidate Send Success")
        }, onError = { error ->
            Log.e(TAG, "Ice Candidate Send failed: $error")
        })
    }

    private fun addIceCandidate(iceCandidate: IceCandidate) {
        Log.d(TAG, "IceCandidate ${iceCandidate.sdp}")
        if (peerConnection.remoteDescription !== null) {
            sendIceCandidate(iceCandidate)
        } else {
            pendingCandidates.add(iceCandidate)
        }
    }

    fun addVideoToView() {
        if (localVideoTrack !== null) {
            val videoItem = VideoItem(
                name = exchange.sid, // You can set a dynamic title
                videoTrack = localVideoTrack, mirror = true
            )
            videoViewAdapter.addOrUpdateItem(videoItem)
        }
    }

    // Close the peer connection and stop the video capturer
    fun close() {
        videoViewAdapter.findAndRemoveItemByName(exchange.sid)
        peerConnection.close()
        peerConnectionFactory.dispose()
        videoCapturer.stopCapture()

    }


    companion object {
        private const val LOCAL_TRACK_ID = "ARDAMSa0"
        private const val LOCAL_AUDIO_TRACK_ID = "ARDAMSa0"
        private const val LOCAL_STREAM_ID = "ARDAMS"
        private const val VIDEO_TRACK_ID = "ARDAMSv0"
    }
}