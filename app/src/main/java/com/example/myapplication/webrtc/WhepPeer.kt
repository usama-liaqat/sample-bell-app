package com.example.myapplication.webrtc

import android.app.Activity
import android.util.Log
import com.example.myapplication.VideoItem
import com.example.myapplication.VideoViewAdapter
import com.example.myapplication.exchange.WHEPExchange
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoTrack

class WHEPPeer(
    private val activity: Activity,
    private val videoViewAdapter: VideoViewAdapter,
    private val exchange: WHEPExchange,
    private val factory: PeerFactory,
) {
    private val TAG = "WHEPPeer"

    private val peerConnection: PeerConnection by lazy { createPeerConnection() }

    private val pendingCandidates = arrayListOf<IceCandidate>()


    private fun createPeerConnection(): PeerConnection {
        val combinedObserver = object : PeerConnectionObserver() {
            override fun onIceCandidate(iceCandidate: IceCandidate?) {
                if (iceCandidate !== null) {
                    addIceCandidate(iceCandidate)
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
        }
        return factory.createPeerConnection(factory.getRTCConfig(), combinedObserver)
    }


    fun connect() {
        createOffer()
    }


    private fun createOffer() {
        val constraints = MediaConstraints()
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        constraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
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

    private fun sendIceCandidate(iceCandidate: IceCandidate){
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
            peerConnection.addIceCandidate(iceCandidate)
        } else {
            pendingCandidates.add(iceCandidate)
        }
    }

    private fun addRemoteTrackToUI(videoTrack: VideoTrack) {
        val videoItem = VideoItem(
            name = exchange.sid, // You can set a dynamic title
            videoTrack = videoTrack,
            mirror = false
        )

        activity.runOnUiThread {
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
    }
}