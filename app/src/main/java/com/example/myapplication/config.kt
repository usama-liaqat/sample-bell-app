package com.example.myapplication

import android.content.Context

object Config {
    lateinit var LIVE_BASE_URL: String
    lateinit var WEB_SOCKET_URI: String
    lateinit var TURN_API_SERVER_URI: String
    lateinit var TURN_API_KEY: String

    fun initialize(context: Context) {
        LIVE_BASE_URL = context.getString(R.string.live_base_uri)
        WEB_SOCKET_URI = context.getString(R.string.websocket_uri)
        TURN_API_SERVER_URI = context.getString(R.string.turn_server_api)
        TURN_API_KEY = context.getString(R.string.turn_api_key)
    }
}