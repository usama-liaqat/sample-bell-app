package com.example.myapplication.webrtc

import android.app.Activity
import android.util.Log
import com.example.myapplication.VideoItem
import com.example.myapplication.VideoViewAdapter
import com.example.myapplication.exchange.WHIPExchange
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.util.UUID


class WHIPPeer(
    private val activity: Activity,
    private val videoCapturer: VideoCapturer,
    private val exchange: WHIPExchange,
    private val videoViewAdapter: VideoViewAdapter,
    private val factory: PeerFactory,

    ) {
    private val TAG = "WHIPPeer"


    private val localStreamID = UUID.randomUUID().toString()
    private val localAudioTrackID = UUID.randomUUID().toString()
    private val localVideoTrackID = UUID.randomUUID().toString()

    private val rootEglBase: EglBase = EglBase.create()

    private val peerConnection: PeerConnection by lazy { createPeerConnection() }

    private var localAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null

    private val localVideoSource by lazy {
        factory.createVideoSource(false)
    }

    private val localAudioSource by lazy {
        factory.createAudioSource(
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

    private fun startCapture() {
        val surfaceTextureHelper = SurfaceTextureHelper.create(
            Thread.currentThread().name,
            rootEglBase.eglBaseContext
        )
        videoCapturer.initialize(surfaceTextureHelper, activity, localVideoSource.capturerObserver)
        videoCapturer.startCapture(1280, 720, 30)
    }

    private fun initPeerConnection(): PeerConnection {
        localVideoTrack = factory.createVideoTrack(
            localVideoTrackID,
            localVideoSource
        )
        localVideoTrack?.setEnabled(true)
        peerConnection.addTrack(localVideoTrack, listOf(localStreamID))

        localAudioTrack = factory.createAudioTrack(
            localAudioTrackID,
            localAudioSource
        )
        localAudioTrack?.setEnabled(true)
        peerConnection.addTrack(localAudioTrack, listOf(localStreamID))

        return peerConnection
    }

    fun connect() {
        addVideoToView()
        createOffer()
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

    private fun removeRemoteTrackFromUI() {
        activity.runOnUiThread {
            videoViewAdapter.findAndRemoveItemByName(exchange.sid)
        }
    }


    // Close the peer connection and stop the video capturer
    fun close() {
        removeRemoteTrackFromUI()
        peerConnection.close()
        videoCapturer.stopCapture()
        rootEglBase.release()
    }
}