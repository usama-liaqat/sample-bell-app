package com.example.myapplication.webrtc

import android.app.Activity
import android.util.Log
import com.example.myapplication.VideoItem
import com.example.myapplication.VideoViewAdapter
import com.example.myapplication.exchange.WHIPExchange
import org.webrtc.AudioTrack
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
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
    private val factory: PeerFactory,

    ) {
    private val TAG = "WHIPPeer"


    private val LOCAL_STREAM_ID = "media_stream_${exchange.sid}"
    private val LOCAL_AUDIO_TRACK_ID = "audio_track_${exchange.sid}"
    private val LOCAL_VIDEO_TRACK_ID = "video_track_${exchange.sid}"

    private val peerConnection: PeerConnection by lazy { createPeerConnection() }

    private var localAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null

    private val localVideoSource by lazy { factory.peerConnectionFactory.createVideoSource(false) }
    private val localAudioSource by lazy {
        factory.peerConnectionFactory.createAudioSource(
            MediaConstraints()
        )
    }
    private val pendingCandidates = arrayListOf<IceCandidate>()


    init {
        startCapture()
        initPeerConnection()
    }


    private fun createPeerConnection(): PeerConnection {
        return factory.createPeerConnection(
            factory.getRTCConfig(),
            object : PeerConnectionObserver() {
                override fun onIceCandidate(iceCandidate: IceCandidate?) {
                    if (iceCandidate !== null) {
                        addIceCandidate(iceCandidate)
                    }
                }
            })
    }

    fun initPeerConnection(): PeerConnection {
        localAudioTrack = factory.peerConnectionFactory.createAudioTrack(
            LOCAL_AUDIO_TRACK_ID,
            localAudioSource
        )

        localVideoTrack = factory.peerConnectionFactory.createVideoTrack(
            LOCAL_VIDEO_TRACK_ID,
            localVideoSource
        )

        val localStream = factory.peerConnectionFactory.createLocalMediaStream(LOCAL_STREAM_ID)

        localStream.addTrack(localVideoTrack)
        localStream.addTrack(localAudioTrack)

        localVideoTrack?.setEnabled(true)
        localAudioTrack?.setEnabled(true)

        peerConnection.addTrack(localVideoTrack, listOf(LOCAL_STREAM_ID))
        peerConnection.addTrack(localAudioTrack, listOf(LOCAL_STREAM_ID))

        return peerConnection
    }

    fun connect() {
        createOffer()
    }

    fun startCapture() {
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,
            factory.rootEglBase.eglBaseContext
        )
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
        videoCapturer.stopCapture()
    }
}