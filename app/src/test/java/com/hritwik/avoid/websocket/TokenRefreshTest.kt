package com.hritwik.avoid.websocket

import com.hritwik.avoid.data.remote.websocket.OkHttpPlaybackWebSocketClient
import com.hritwik.avoid.data.remote.websocket.PlaybackEventParser
import com.hritwik.avoid.data.remote.websocket.ExponentialBackoff
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertTrue
import io.mockk.every
import io.mockk.mockk

class TokenRefreshTest {
    @Test
    fun `auth failure triggers callback`() = runTest {
        var listener: WebSocketListener? = null
        val webSocket = mockk<WebSocket>(relaxed = true)
        val client = mockk<OkHttpClient> {
            every { newWebSocket(any(), any()) } answers {
                listener = secondArg()
                webSocket
            }
        }
        var called = false
        val wsClient = OkHttpPlaybackWebSocketClient(client, PlaybackEventParser(), CoroutineScope(Dispatchers.Unconfined), ExponentialBackoff())
        wsClient.start("http://localhost", "auth", "u", "d") { called = true }
        val response = Response.Builder()
            .request(Request.Builder().url("http://localhost").build())
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("unauth")
            .build()
        listener!!.onFailure(webSocket, IOException(), response)
        assertTrue(called)
    }
}
