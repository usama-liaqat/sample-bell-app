package com.example.myapplication.webrtc

import android.app.Activity
import android.util.Log
import com.example.myapplication.VideoItem
import com.example.myapplication.VideoViewAdapter
import com.example.myapplication.exchange.SocketExchange
import org.webrtc.AudioTrack
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import java.util.UUID


class Peer(
    private val id: String,
    private val publish: Boolean,
    private val activity: Activity,
    private val videoCapturer: VideoCapturer,
    private val factory: PeerFactory,
    private var socketExchange: SocketExchange,
    private val videoViewAdapter: VideoViewAdapter,
    private val rootEglBase: EglBase = EglBase.create()
    ) {

    private val TAG = "Peer"

    private val localStreamID = UUID.randomUUID().toString()
    private val localAudioTrackID = UUID.randomUUID().toString()
    private val localVideoTrackID = UUID.randomUUID().toString()



    private val peerConnection: PeerConnection by lazy { createPeerConnection() }

    private var localAudioTrack: AudioTrack? = null

    private var localVideoTrack: VideoTrack? = null

    private val localVideoSource by lazy { factory.createVideoSource(false) }
    private val localAudioSource by lazy {
        factory.createAudioSource(
            MediaConstraints()
        )
    }
    private val pendingCandidates = arrayListOf<IceCandidate>()

    init {
        if (publish) {
            startCapture()
            initTracks()
            addLocalTrackToView()
        }
    }


    private fun createPeerConnection(): PeerConnection {
        return factory.createPeerConnection(factory.getRTCConfig(factory.iceServersList()),
            object : PeerConnectionObserver() {
                override fun onIceCandidate(iceCandidate: IceCandidate?) {
                    if (iceCandidate !== null) {
                        socketExchange.sendCandidate(id, iceCandidate)
                    }
                }

                override fun onAddStream(mediaStream: MediaStream?) {
                    Log.d(
                        TAG, "***** PeerConnectionObserver onAddStream fun invoked *****"
                    )

                    if (mediaStream !== null && mediaStream.videoTracks !== null && mediaStream.videoTracks.isNotEmpty()) {
                        val remoteVideoTrack = mediaStream.videoTracks[0]
                        if (remoteVideoTrack !== null) {
                            remoteVideoTrack.setEnabled(true)
                            addRemoteTrackToUI(remoteVideoTrack)
                        }

                    }
                }

                override fun onTrack(transceiver: RtpTransceiver?) {
                    super.onTrack(transceiver)
                    Log.d(TAG, "***** PeerConnectionObserver onTrack fun invoked = $ *****")

                    if (transceiver?.receiver?.track() is VideoTrack) {
                        val remoteVideoTrack = transceiver.receiver.track() as VideoTrack
                        remoteVideoTrack.setEnabled(true)
                        addRemoteTrackToUI(remoteVideoTrack)
                    }
                }

                override fun onRemoveTrack(receiver: RtpReceiver?) {
                    if (receiver?.track() is VideoTrack) {
                        removeTrackFromUI(id)
                    }

                }

                override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
                    val shouldRemoveTracks = arrayOf(
                        PeerConnection.IceConnectionState.DISCONNECTED,
                        PeerConnection.IceConnectionState.CLOSED
                    ).contains(iceConnectionState)

                    if (shouldRemoveTracks) {
                        removeTrackFromUI(id)
                        removeTrackFromUI(socketExchange.sid)
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


    private fun initTracks() {
        localVideoTrack = factory.createVideoTrack(
            localVideoTrackID,
            localVideoSource
        )
        peerConnection.addTrack(localVideoTrack, listOf(localStreamID))
        localVideoTrack?.setEnabled(true)


        localAudioTrack = factory.createAudioTrack(
            localAudioTrackID,
            localAudioSource
        )
        peerConnection.addTrack(localAudioTrack, listOf(localStreamID))
        localAudioTrack?.setEnabled(true)
    }



    fun createOffer() {
        val constraints = MediaConstraints()
        peerConnection.createOffer(object : SdpObserver {
            override fun onCreateSuccess(offer: SessionDescription?) {
                Log.d(TAG, "Offer Description created")
                if (offer != null) {
                    socketExchange.sendOffer(id, offer)
                    peerConnection.setLocalDescription(object : SdpObserver {
                        override fun onCreateSuccess(sdp: SessionDescription?) {
                            Log.e(TAG, "Local Description create success")
                        }

                        override fun onCreateFailure(error: String?) {
                            Log.e(TAG, "Local Description creation failed: $error")
                        }

                        override fun onSetSuccess() {
                            Log.e(TAG, "Local Description set success")
                        }

                        override fun onSetFailure(error: String?) {
                            Log.e(TAG, "Local Description set failed: $error")
                        }
                    }, offer)
                }
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Offer Description creation failed: $error")
            }

            override fun onSetSuccess() {
                Log.e(TAG, "Offer Description set success")

            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Offer Description set failed: $error")

            }
        }, constraints)
    }

    fun createAnswer(offer: SessionDescription) {
        val constraints = MediaConstraints()
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.e(TAG, "Remote Description create success")
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Remote Description creation failed: $error")
            }

            override fun onSetSuccess() {
                Log.e(TAG, "Remote Description set success")

                peerConnection.createAnswer(object : SdpObserver {
                    override fun onCreateSuccess(answer: SessionDescription?) {
                        Log.e(TAG, "Answer Description create success")
                        if (answer != null) {
                            socketExchange.sendAnswer(id, answer)
                            peerConnection.setLocalDescription(object : SdpObserver {
                                override fun onCreateSuccess(sdp: SessionDescription?) {
                                    Log.e(TAG, "Local Description create success")
                                }

                                override fun onCreateFailure(error: String?) {
                                    Log.e(TAG, "Local Description creation failed: $error")
                                }

                                override fun onSetSuccess() {
                                    Log.e(TAG, "Local Description set success")
                                }

                                override fun onSetFailure(error: String?) {
                                    Log.e(TAG, "Local Description set failed: $error")
                                }
                            }, answer)
                        }
                    }

                    override fun onCreateFailure(error: String?) {
                        Log.e(TAG, "Answer Description creation failed: $error")
                    }

                    override fun onSetSuccess() {
                        Log.e(TAG, "Answer Description set success")

                    }

                    override fun onSetFailure(error: String?) {
                        Log.e(TAG, "Answer Description set failed: $error")

                    }
                }, constraints)
                pendingCandidates.forEach { candidate ->
                    peerConnection.addIceCandidate(candidate)
                }
                pendingCandidates.clear()
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Remote Description set failed: $error")

            }
        }, offer)

    }

    fun addRemoteAnswer(answer: SessionDescription) {
        peerConnection.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(sdp: SessionDescription?) {
                Log.e(TAG, "Remote Description create success")
            }

            override fun onCreateFailure(error: String?) {
                Log.e(TAG, "Remote Description creation failed: $error")
            }

            override fun onSetSuccess() {
                Log.e(TAG, "Remote Description set success")
                pendingCandidates.forEach { candidate ->
                    peerConnection.addIceCandidate(candidate)
                }
                pendingCandidates.clear()
            }

            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Remote Description set failed: $error")
            }
        }, answer)
    }


    fun addIceCandidate(iceCandidate: IceCandidate) {
        if (peerConnection.remoteDescription !== null) {
            peerConnection.addIceCandidate(iceCandidate)
        } else {
            pendingCandidates.add(iceCandidate)
        }
    }

    private fun addLocalTrackToView() {
        activity.runOnUiThread {
            if (publish && localVideoTrack?.enabled() == true) {
                val videoItem = VideoItem(
                    name = socketExchange.sid, // You can set a dynamic title
                    videoTrack = localVideoTrack, mirror = true
                )
                videoViewAdapter.addOrUpdateItem(videoItem)
            }
        }
    }

    private fun addRemoteTrackToUI(videoTrack: VideoTrack) {
        activity.runOnUiThread {
            val videoItem = VideoItem(
                name = id, // You can set a dynamic title
                videoTrack = videoTrack, mirror = false
            )
            videoViewAdapter.addOrUpdateItem(videoItem)
        }
    }

    private fun removeTrackFromUI(name: String) {
        activity.runOnUiThread {
            videoViewAdapter.findAndRemoveItemByName(name)
        }
    }



    fun close() {
        if(peerConnection.connectionState() === PeerConnection.PeerConnectionState.CONNECTED) {
            peerConnection.close()
            videoCapturer.stopCapture()
        }
        removeTrackFromUI(id)
        removeTrackFromUI(socketExchange.sid)
    }
}