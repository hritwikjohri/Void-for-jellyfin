package com.hritwik.avoid.data.remote.websocket

import kotlinx.coroutines.flow.Flow


interface PlaybackWebSocketClient {
    val events: Flow<PlaybackEvent>
    fun start(
        url: String,
        authHeader: String,
        userId: String,
        deviceId: String,
        onAuthFailed: () -> Unit = {}
    )
    fun stop()
}
