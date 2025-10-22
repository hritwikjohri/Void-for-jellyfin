package com.hritwik.avoid.data.prefetch

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.utils.helpers.NetworkHelper
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request

class PrefetchWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PrefetchEntryPoint {
        fun preferencesManager(): PreferencesManager
        fun libraryRepository(): LibraryRepository
        fun mediaItemDao(): MediaItemDao
        fun okHttpClient(): OkHttpClient
        fun networkMonitor(): NetworkMonitor
        fun networkHelper(): NetworkHelper
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            PrefetchEntryPoint::class.java
        )
        val prefs = entryPoint.preferencesManager()
        val repository = entryPoint.libraryRepository()
        val dao = entryPoint.mediaItemDao()
        val client = entryPoint.okHttpClient()
        val monitor = entryPoint.networkMonitor()
        val helper = entryPoint.networkHelper()

        val connected = monitor.isConnected.first()
        val wifiOnly = prefs.getCacheWifiOnly().first()
        val isWifi = helper.isWifiConnected()
        if (!connected || (wifiOnly && !isWifi)) {
            return Result.retry()
        }

        val userId = prefs.getUserId().first() ?: return Result.success()
        val token = prefs.getAccessToken().first() ?: return Result.success()
        val serverUrl = prefs.getServerUrl().first() ?: return Result.success()

        when (val result = repository.getNextUpEpisodes(userId, token, limit = 10)) {
            is NetworkResult.Success -> {
                val episodes = result.data.map { item ->
                    when (val detail = repository.getMediaItemDetail(userId, item.id, token)) {
                        is NetworkResult.Success -> detail.data
                        else -> item
                    }
                }
                dao.insertMediaItems(episodes.map { it.toEntity(userId) })
                episodes.forEach { item ->
                    prefetchImage(client, serverUrl, item)
                }
                return Result.success()
            }
            else -> return Result.retry()
        }
    }

    private fun prefetchImage(client: OkHttpClient, serverUrl: String, item: MediaItem) {
        val tag = item.primaryImageTag ?: item.thumbImageTag ?: return
        val base = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val url = "${base}Items/${item.id}/Images/Primary?tag=$tag"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute().use { it.body?.close() }
        } catch (_: Exception) {
            
        }
    }

    private fun MediaItem.toEntity(userId: String) = com.hritwik.avoid.data.local.database.entities.MediaItemEntity(
        id = id,
        name = name,
        title = title,
        type = type,
        overview = overview,
        year = year,
        communityRating = communityRating,
        runTimeTicks = runTimeTicks,
        primaryImageTag = primaryImageTag,
        thumbImageTag = thumbImageTag,
        backdropImageTags = backdropImageTags,
        genres = genres,
        isFolder = isFolder,
        childCount = childCount,
        libraryId = null,
        userId = userId,
        isFavorite = userData?.isFavorite ?: false,
        playbackPositionTicks = userData?.playbackPositionTicks ?: 0L,
        playCount = userData?.playCount ?: 0,
        played = userData?.played ?: false,
        lastPlayedDate = userData?.lastPlayedDate,
        isWatchlist = userData?.isWatchlist ?: false,
        pendingFavorite = userData?.pendingFavorite ?: false,
        pendingPlayed = userData?.pendingPlayed ?: false,
        pendingWatchlist = userData?.pendingWatchlist ?: false,
        taglines = taglines
    )

    companion object {
        private const val WORK_NAME = "PrefetchNextMedia"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PrefetchWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}

