package com.hritwik.avoid.data.network

import com.hritwik.avoid.utils.constants.AppConstants
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl

@Singleton
class CdnInterceptor @Inject constructor() : Interceptor {

    private val cdnBaseUrl = AppConstants.CDN_BASE_URL.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url

        if (!isMediaRequest(url.encodedPath)) {
            return chain.proceed(originalRequest)
        }

        val cdnUrl = url.newBuilder()
            .scheme(cdnBaseUrl.scheme)
            .host(cdnBaseUrl.host)
            .port(cdnBaseUrl.port)
            .build()
        val cdnRequest = originalRequest.newBuilder().url(cdnUrl).build()

        return chain.proceed(cdnRequest)
    }

    private fun isMediaRequest(path: String): Boolean {
        val lower = path.lowercase()
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
               lower.endsWith(".png") || lower.endsWith(".webp") ||
               lower.endsWith(".mp4") || lower.endsWith(".mkv") ||
               lower.contains("/images/") || lower.contains("/videos/")
    }
}
