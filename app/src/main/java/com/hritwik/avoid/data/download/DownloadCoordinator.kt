package com.hritwik.avoid.data.download

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.core.ServiceManager
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.utils.helpers.normalizeUuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DownloadCoordinator"

@OptIn(UnstableApi::class)
@Singleton
class DownloadCoordinator @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val downloadDao: DownloadDao,
    private val preferencesManager: PreferencesManager,
    private val serviceManager: ServiceManager,
) {

    suspend fun startDownload(
        mediaItem: MediaItem,
        request: DownloadRequest,
        serverUrl: String,
        accessToken: String,
        priority: Int = 0,
        mediaSourceId: String? = null,
        userId: String? = null,
    ) {
        if (mediaItem.type.equals("Season", ignoreCase = true)) {
            queueSeasonDownloads(
                season = mediaItem,
                request = request,
                serverUrl = serverUrl,
                accessToken = accessToken,
                priority = priority,
                userId = userId
            )
        } else {
            serviceManager.startDownload(
                mediaItem = mediaItem,
                serverUrl = serverUrl,
                accessToken = accessToken,
                request = request,
                priority = priority,
                mediaSourceId = mediaSourceId
            )
        }
    }

    private suspend fun queueSeasonDownloads(
        season: MediaItem,
        request: DownloadRequest,
        serverUrl: String,
        accessToken: String,
        priority: Int,
        userId: String?,
    ) {
        val resolvedUserId = userId?.takeIf { it.isNotBlank() }
            ?: preferencesManager.getUserId().first()

        if (resolvedUserId.isNullOrBlank()) {
            Log.w(TAG, "Unable to start season download without a user id")
            return
        }

        when (val result = libraryRepository.getEpisodes(resolvedUserId, season.id, accessToken)) {
            is NetworkResult.Success -> {
                val episodes = result.data
                if (episodes.isEmpty()) {
                    Log.w(TAG, "No episodes found for season ${season.id}")
                    return
                }

                val storedDownloads = withContext(Dispatchers.IO) {
                    downloadDao.getAllDownloads().first()
                }

                val storedMediaIds = storedDownloads.map { it.mediaId }.toSet()
                val storedSourceIds = storedDownloads.mapNotNull { it.mediaSourceId }.toSet()
                val activeDownloads = serviceManager.downloads.value
                val activeSourceIds = activeDownloads.keys
                val activeMediaIds = activeDownloads.values.map { normalizeUuid(it.mediaItem.id) }.toSet()

                var queuedCount = 0
                episodes.forEach { episode ->
                    val normalizedEpisodeId = normalizeUuid(episode.id)
                    val primarySourceId = episode.getPrimaryMediaSource()?.id
                    val effectiveSourceId = primarySourceId ?: normalizedEpisodeId

                    val alreadyQueued = normalizedEpisodeId in storedMediaIds ||
                        normalizedEpisodeId in activeMediaIds ||
                        effectiveSourceId in storedSourceIds ||
                        effectiveSourceId in activeSourceIds

                    if (alreadyQueued) {
                        return@forEach
                    }

                    serviceManager.startDownload(
                        mediaItem = episode,
                        serverUrl = serverUrl,
                        accessToken = accessToken,
                        request = request,
                        priority = priority + queuedCount,
                        mediaSourceId = primarySourceId
                    )
                    queuedCount++
                }

                Log.d(
                    TAG,
                    "Queued ${'$'}queuedCount/${'$'}{episodes.size} episodes for season ${'$'}{season.id}"
                )
            }

            is NetworkResult.Error -> {
                Log.e(
                    TAG,
                    "Failed to fetch episodes for season ${'$'}{season.id}: ${'$'}{result.error}"
                )
            }

            else -> Unit
        }
    }
}
