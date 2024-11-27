package com.example.myapplication.exchange

import android.util.Log
import com.example.myapplication.Config
import kotlinx.coroutines.DelicateCoroutinesApi
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.WebSocketListener
import org.webrtc.SessionDescription

class SocketExchange(private val name: String) {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).build()
    private lateinit var webSocket: WebSocket

    private val _eventsFlow = MutableSharedFlow<Pair<String, JSONObject>>()
    val events: SharedFlow<Pair<String, JSONObject>> = _eventsFlow


    private fun onOffer(json: JSONObject){
        triggerEventSafely("offer", json)
    }

    private fun onAnswer(json: JSONObject){
        triggerEventSafely("answer", json)
    }

    private fun onCandidate(json: JSONObject){
        triggerEventSafely("candidate", json)
    }

    fun connect(){
        val request = Request.Builder().url(Config.WEB_SOCKET_URI).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.e(TAG, "WebSocket connection Open")

                webSocket.send("{\"type\": \"register\", \"name\": \"${name}\"}")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket connection failed: ${t.message}", t)
                response?.let {
                    Log.e(TAG, "Response code: ${it.code}, message: ${it.message}")
                }
            }
            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.e(TAG, "WebSocket Message $text")

                val json = JSONObject(text)
                when (json.getString("type")) {
                    "offer" -> {
                        onOffer(json)
                    }
                    "answer" -> {
                        onAnswer(json)
                    }
                    "candidate" -> {
                        onCandidate(json)
                    }
                }
            }
        })

    }

    suspend fun triggerEvent(name: String, data:JSONObject){
        _eventsFlow.emit(name to data)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun triggerEventSafely(name: String, data: JSONObject) {
        kotlinx.coroutines.GlobalScope.launch {
            triggerEvent(name, data)
        }
    }

    fun sendOffer(
        target:String,
        sdp: SessionDescription
    ) {
        val offer = JSONObject()
        offer.put("type", "offer")
        offer.put("sdp", sdp.description)
        offer.put("from", name)
        offer.put("target", target)
        webSocket.send(offer.toString())
    }

    fun sendAnswer(
        target:String,
        sdp: SessionDescription
    ) {
        val answer = JSONObject()
        answer.put("type", "answer")
        answer.put("sdp", sdp.description)
        answer.put("from", name)
        answer.put("target", target)
        webSocket.send(answer.toString())
    }

    fun sendCandidate(
        target:String,
        candidate: org.webrtc.IceCandidate
    ) {

        val ice = JSONObject()
        ice.put("type", "candidate")
        ice.put("candidate", candidate.sdp)
        ice.put("sdpMLineIndex", candidate.sdpMLineIndex)
        ice.put("sdpMid", candidate.sdpMid)
        ice.put("from", name)
        ice.put("target", target)
        webSocket.send(ice.toString())
    }


    companion object{
        const val TAG = "SocketExchange"
    }
}