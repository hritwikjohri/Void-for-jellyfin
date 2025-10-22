package com.hritwik.avoid.data.remote.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow


class FakePlaybackWebSocketClient : PlaybackWebSocketClient {
    private val _events = MutableSharedFlow<PlaybackEvent>()
    override val events: Flow<PlaybackEvent> = _events.asSharedFlow()

    fun send(event: PlaybackEvent) { _events.tryEmit(event) }

    override fun start(
        url: String,
        authHeader: String,
        userId: String,
        deviceId: String,
        onAuthFailed: () -> Unit
    ) {
        
    }

    override fun stop() { }
}
