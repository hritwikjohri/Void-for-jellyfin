package com.hritwik.avoid.data.prefetch

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.hritwik.avoid.data.repository.ContinueWatchingStore
import com.hritwik.avoid.di.ApplicationScope
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.data.cache.CacheManager
import com.hritwik.avoid.utils.helpers.NetworkHelper
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import okhttp3.OkHttpClient
import okhttp3.Request


@Singleton
class PrefetchManager @Inject constructor(
    private val continueWatchingStore: ContinueWatchingStore,
    @ApplicationScope private val scope: CoroutineScope,
    private val preferencesManager: PreferencesManager,
    private val libraryRepository: LibraryRepository,
    private val cacheManager: CacheManager,
    private val networkHelper: NetworkHelper,
    private val okHttpClient: OkHttpClient,
) {
    private var started = false

    fun start() {
        if (started) return
        started = true
        scope.launch {
            continueWatchingStore.items.collectLatest { list ->
                if (list.any { it.seriesId != null }) {
                    maybePrefetch()
                }
            }
        }
    }

    private suspend fun maybePrefetch() {
        val enabled = preferencesManager.getPrefetchEnabled().first()
        if (!enabled) return

        val wifiOnly = preferencesManager.getCacheWifiOnly().first()
        val networkOk =
            networkHelper.isNetworkConnected() && (!wifiOnly || networkHelper.isWifiConnected())
        if (!networkOk) return

        prefetchUpcomingEpisodes()
    }

    private suspend fun prefetchUpcomingEpisodes() {
        val userId = preferencesManager.getUserId().first() ?: return
        val token = preferencesManager.getAccessToken().first() ?: return
        val serverUrl = preferencesManager.getServerUrl().first() ?: return

        when (val result = libraryRepository.getNextUpEpisodes(userId, token, limit = 10)) {
            is NetworkResult.Success -> {
                result.data.forEach { item ->
                    val detail = when (val detailResult =
                        libraryRepository.getMediaItemDetail(userId, item.id, token)) {
                        is NetworkResult.Success -> detailResult.data
                        else -> item
                    }
                    cacheManager.putMetadata(detail)
                    prefetchPoster(serverUrl, detail)
                }
            }
            else -> { }
        }
    }

    private fun prefetchPoster(serverUrl: String, item: MediaItem) {
        val tag = item.primaryImageTag ?: item.thumbImageTag ?: return
        val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val url = "${base}Items/${item.id}/Images/Primary?tag=$tag"
        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes() ?: return
                cacheManager.putPoster(item.id, bytes)
            }
        } catch (_: Exception) {
            
        }
    }
}

