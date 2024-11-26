package com.example.myapplication.webrtc


data class RTCIceServer(
    val credential: String? = null,
    val url: String? = null, // Deprecated
    val urls: List<String>? = null,
    val username: String? = null
)

data class RTCIceCandidateInit(
    val candidate: String? = null,
    val sdpMLineIndex: Int? = null,
    val sdpMid: String? = null,
    val usernameFragment: String? = null
)