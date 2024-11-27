package com.example.myapplication.exchange

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class WHEPExchange(private val baseURI: String, private val sid: String) {
    private val client = OkHttpClient()
    private var sessionPath: String = ""

    fun sendOffer(
        offerSdp: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val mediaType = "application/sdp".toMediaType()
        val body = offerSdp.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$baseURI/$sid/whep")
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

    fun sendLocalCandidates(
        candidate: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val mediaType = "application/trickle-ice-sdpfrag".toMediaType()
        val body = candidate.toRequestBody(mediaType)
        val request = Request.Builder()
            .url("$baseURI$sessionPath")
            .patch(body)
            .header("If-Match", "*")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                when (response.code) {
                    204 -> onSuccess()
                    404 -> onError("Stream not found")
                    else -> onError("Bad status code: ${response.code}")
                }
            }
        })
    }
}
