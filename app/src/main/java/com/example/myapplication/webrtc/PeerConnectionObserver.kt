package com.example.myapplication.webrtc

import org.webrtc.DataChannel
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.RtpReceiver
import org.webrtc.IceCandidate
import org.webrtc.RtpTransceiver

open class PeerConnectionObserver : PeerConnection.Observer {
    override fun onIceCandidate(iceCandidate: IceCandidate?) {
    }

    override fun onDataChannel(dataChannel: DataChannel?) {
    }

    override fun onIceConnectionReceivingChange(iceConnectionReceiving: Boolean) {
    }

    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState?) {
    }

    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState?) {
    }

    override fun onAddStream(mediaStream: MediaStream?) {
    }

    override fun onSignalingChange(signalingState: PeerConnection.SignalingState?) {
    }

    override fun onIceCandidatesRemoved(iceCandidates: Array<out IceCandidate>?) {
    }

    override fun onRemoveStream(mediaStream: MediaStream?) {
    }

    override fun onRenegotiationNeeded() {
    }

    override fun onAddTrack(receiver: RtpReceiver?, mediaStreams: Array<out MediaStream>?) {
    }

    override fun onTrack(transceiver: RtpTransceiver?) {

    }

    override fun onRemoveTrack(receiver: RtpReceiver?) {

    }
}