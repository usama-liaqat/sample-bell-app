package com.example.myapplication.exchange

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.webrtc.SessionDescription
import java.io.IOException

class WHIPExchange(private val baseURI: String, val sid: String) {
    private val client = OkHttpClient()
    private var sessionPath: String = ""

    fun sendOffer(offer: SessionDescription, onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        val mediaType = "application/sdp".toMediaType()
        val body = offer.description.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$baseURI/$sid/whip")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    201 -> {
                        sessionPath = response.header("location") ?: ""
                        onSuccess(response.body?.string() ?: "")
                    }

                    404 -> onError("Stream not found")
                    400 -> {
                        val error = response.body?.string() ?: "Unknown error"
                        onError(error)
                    }

                    else -> onError("Bad status code: ${response.code}")
                }
            }
        })
    }

    fun sendLocalCandidates(candidate: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val mediaType = "application/trickle-ice-sdpfrag".toMediaType()
        val body = candidate.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$baseURI/$sessionPath")
            .patch(body)
            .header("If-Match", "*")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.code == 204) {
                    onSuccess()
                } else {
                    onError("Bad status code: ${response.code}")
                }
            }
        })
    }
}
