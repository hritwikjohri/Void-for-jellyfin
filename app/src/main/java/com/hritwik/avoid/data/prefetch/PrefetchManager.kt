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
import com.hritwik.avoid.data.connection.ServerConnectionManager
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
    private val serverConnectionManager: ServerConnectionManager,
) {
    private var started = false
    private var hasPrefetchedForSession = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            preferencesManager.isLoggedIn().collectLatest { loggedIn ->
                if (loggedIn) {
                    maybePrefetch()
                } else {
                    hasPrefetchedForSession = false
                }
            }
        }

        scope.launch {
            continueWatchingStore.items.collectLatest { items ->
                // Keep metadata cache warm for resume carousel
                items.forEach(cacheManager::putMetadata)
            }
        }
    }

    /**
     * Prefetch home screen data on app launch for improved initial load performance.
     * This fetches and caches the most commonly accessed data to leverage stale-while-revalidate.
     */
    suspend fun prefetchHomeScreenData() {
        if (serverConnectionManager.state.value.isOffline) return
        val userId = preferencesManager.getUserId().first() ?: return
        val token = preferencesManager.getAccessToken().first() ?: return
        val serverUrl = preferencesManager.getServerUrl().first() ?: return

        val result = libraryRepository.getHomeScreenData(
            userId = userId,
            accessToken = token,
            limit = HOME_PREFETCH_LIMIT
        )

        if (result is NetworkResult.Success) {
            val home = result.data
            val posterCandidates = (home.latestItems + home.resumeItems + home.nextUpItems + home.latestEpisodes)
                .distinctBy { it.id }
                .take(MAX_METADATA_ITEMS)

            posterCandidates.forEach { item ->
                cacheManager.putMetadata(item)
                prefetchPoster(serverUrl, item)
            }
        }
    }

    private suspend fun maybePrefetch() {
        if (hasPrefetchedForSession) return
        if (!networkHelper.isNetworkAvailable()) return
        if (serverConnectionManager.state.value.isOffline) return

        hasPrefetchedForSession = true
        prefetchHomeScreenData()
    }

    private suspend fun prefetchUpcomingEpisodes() {
        // Disabled
    }

    private fun prefetchPoster(serverUrl: String, item: MediaItem) {
        val tag = item.primaryImageTag ?: item.thumbImageTag ?: return
        val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val url = "${base}Items/${item.id}/Images/Primary?tag=$tag"

        val request = Request.Builder().url(url).build()
        try {
            okHttpClient.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes()
                if (bytes != null && bytes.isNotEmpty()) {
                    cacheManager.putPoster(item.id, bytes)
                }
            }
        } catch (_: Exception) {
            // Ignore prefetch failures; they'll be retried on-demand
        }
    }

    companion object {
        private const val HOME_PREFETCH_LIMIT = 10
        private const val MAX_METADATA_ITEMS = 24
    }
}

