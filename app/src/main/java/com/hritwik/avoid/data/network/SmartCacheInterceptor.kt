package com.hritwik.avoid.data.network

import okhttp3.Interceptor
import okhttp3.Response

class SmartCacheInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        return response.newBuilder()
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .build()
    }
}
