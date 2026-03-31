package com.hritwik.avoid.data.network

import com.hritwik.avoid.utils.constants.AppConstants
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.HttpUrl.Companion.toHttpUrl

@Singleton
class CdnInterceptor @Inject constructor() : Interceptor {

    private val cdnBaseUrl = AppConstants.CDN_BASE_URL.toHttpUrl()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url

        if (!shouldRewriteToCdn(url)) {
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

    private fun shouldRewriteToCdn(url: HttpUrl): Boolean {
        if (url.host.equals(cdnBaseUrl.host, ignoreCase = true)) {
            return false
        }

        val segments = url.pathSegments
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }

        val lastSegment = segments.lastOrNull()
        val containsVideosSegment = segments.contains("videos")
        if (containsVideosSegment && lastSegment?.startsWith("stream") == true) {
            return false
        }

        val path = url.encodedPath.lowercase()
        return path.endsWith(".jpg") || path.endsWith(".jpeg") ||
            path.endsWith(".png") || path.endsWith(".webp") ||
            path.endsWith(".mp4") || path.endsWith(".mkv")
    }
}
