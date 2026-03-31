package com.hritwik.avoid.data.network

import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.CacheControl
import okhttp3.Interceptor
import okhttp3.Response
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Singleton
class OfflineCacheInterceptor @Inject constructor(
    private val preferencesManager: PreferencesManager,
    @ApplicationScope private val scope: CoroutineScope
) : Interceptor {
    @Volatile
    private var offlineMode: Boolean = false

    init {
        scope.launch {
            preferencesManager.getOfflineMode().collect { enabled ->
                offlineMode = enabled
            }
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val offline = offlineMode
        val request = if (offline) {
            chain.request()
                .newBuilder()
                .cacheControl(CacheControl.FORCE_NETWORK)
                .build()
        } else {
            chain.request()
        }
        val response = chain.proceed(request)
        if (!offline) return response
        return response.newBuilder()
            .header("Cache-Control", "no-cache, no-store, must-revalidate")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .build()
    }
}
