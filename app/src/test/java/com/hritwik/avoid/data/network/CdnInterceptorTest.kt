package com.hritwik.avoid.data.network

import com.hritwik.avoid.utils.constants.AppConstants
import kotlin.test.assertEquals
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okio.Timeout
import org.junit.Test
import java.util.concurrent.TimeUnit

class CdnInterceptorTest {

    @Test
    fun `request is redirected to cdn`() {
        val interceptor = CdnInterceptor()
        val request = Request.Builder()
            .url("https://origin.example.com/image.jpg")
            .build()
        val chain = FakeChain(request)

        interceptor.intercept(chain)
        assertEquals(AppConstants.CDN_BASE_URL.toHttpUrl().host, chain.request().url.host)
    }

    private class FakeChain(initialRequest: Request) : Interceptor.Chain {
        private var currentRequest: Request = initialRequest

        override fun request(): Request = currentRequest

        override fun proceed(request: Request): Response {
            currentRequest = request
            val delay = if (request.url.host == AppConstants.CDN_BASE_URL.toHttpUrl().host) 10L else 100L
            Thread.sleep(delay)
            return Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body("body".toResponseBody())
                .build()
        }

        override fun connection(): Connection? = null
        override fun call(): Call = object : Call {
            override fun request(): Request = currentRequest
            override fun execute(): Response = throw UnsupportedOperationException()
            override fun enqueue(responseCallback: Callback) {}
            override fun cancel() {}
            override fun isExecuted(): Boolean = true
            override fun isCanceled(): Boolean = false
            override fun timeout(): Timeout = Timeout.NONE
            override fun clone(): Call = this
        }

        override fun connectTimeoutMillis(): Int = 0
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 0
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 0
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }
}
