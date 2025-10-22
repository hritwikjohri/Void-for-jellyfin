package com.hritwik.avoid.data.network

import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

@Singleton
class OfflineCacheInterceptor @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val preferencesManager: PreferencesManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (request.method.equals("GET", ignoreCase = true) && !networkMonitor.isConnected.value) {
            val maxStaleDays = runBlocking { preferencesManager.getMaxStaleDays().first() }
            val maxStale = TimeUnit.DAYS.toSeconds(maxStaleDays.toLong())
            request = request.newBuilder()
                .cacheControl(CacheControl.FORCE_CACHE)
                .header("Cache-Control", "public, only-if-cached, max-stale=$maxStale")
                .build()
        }
        return chain.proceed(request)
    }
}
