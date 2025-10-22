package com.hritwik.avoid.data.remote.websocket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString


class OkHttpPlaybackWebSocketClient(
    private val client: OkHttpClient,
    private val parser: PlaybackEventParser,
    private val scope: CoroutineScope,
    private val backoff: ExponentialBackoff = ExponentialBackoff()
) : PlaybackWebSocketClient {

    private val _events = MutableSharedFlow<PlaybackEvent>(extraBufferCapacity = 64)
    override val events = _events.asSharedFlow()

    private var webSocket: WebSocket? = null
    private var url: String = ""
    private var header: String = ""
    private var userId: String = ""
    private var deviceId: String = ""
    private var connectJob: Job? = null
    private val mutex = Mutex()
    private var stopped = false

    private var authFailed: () -> Unit = {}

    override fun start(
        url: String,
        authHeader: String,
        userId: String,
        deviceId: String,
        onAuthFailed: () -> Unit
    ) {
        this.url = url
        this.header = authHeader
        this.userId = userId
        this.deviceId = deviceId
        this.authFailed = onAuthFailed
        stopped = false
        connectJob?.cancel()
        connectJob = scope.launch { connectLoop() }
    }

    override fun stop() {
        stopped = true
        connectJob?.cancel()
        connectJob = null
        webSocket?.close(1000, null)
        webSocket = null
        backoff.reset()
    }

    private suspend fun connectLoop() {
        while (!stopped) {
            val request = Request.Builder()
                .url(url)
                .header("X-Emby-Authorization", header)
                .build()
            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) {
                    backoff.reset()
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    parser.parse(text, userId, deviceId)?.let { _events.tryEmit(it) }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    reconnectLater()
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) {
                    if (response?.code == 401) authFailed()
                    reconnectLater()
                }
            }
            mutex.withLock { webSocket = client.newWebSocket(request, listener) }
            
            return
        }
    }

    private fun reconnectLater() {
        if (stopped) return
        scope.launch {
            val delayDuration = backoff.nextDelay()
            delay(delayDuration)
            connectLoop()
        }
    }
}
