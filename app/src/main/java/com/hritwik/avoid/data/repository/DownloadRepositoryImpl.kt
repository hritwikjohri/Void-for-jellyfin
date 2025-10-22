package com.hritwik.avoid.data.repository

import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.entities.DownloadEntity
import com.hritwik.avoid.domain.model.download.Download
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.domain.repository.DownloadRepository
import com.hritwik.avoid.utils.helpers.normalizeUuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRepositoryImpl @Inject constructor(
    private val downloadDao: DownloadDao
) : DownloadRepository {

    override fun getDownloadsByStatus(status: String): Flow<List<Download>> {
        return downloadDao.getDownloadsByStatus(status).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getDownload(mediaId: String): Download? {
        val normId = normalizeUuid(mediaId)
        return downloadDao.getDownloadByMediaId(normId)?.toDomain()
            ?: downloadDao.getDownloadByMediaSourceId(mediaId)?.toDomain()
    }

    override suspend fun insertDownload(download: Download) {
        downloadDao.insertDownload(download.toEntity())
    }

    override suspend fun updateDownload(download: Download) {
        downloadDao.updateDownload(download.toEntity())
    }

    override suspend fun deleteDownload(download: Download) {
        downloadDao.deleteDownload(download.toEntity())
    }

    override fun getAllDownloads(): Flow<List<Download>> {
        return downloadDao.getAllDownloads().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private fun DownloadEntity.toDomain(): Download = Download(
        mediaId = mediaId,
        title = title,
        requestUri = requestUri,
        progress = progress.coerceIn(0f, 100f),
        type = type,
        status = status,
        downloadedBytes = downloadedBytes,
        filePath = filePath,
        checksum = checksum,
        mediaSourceId = mediaSourceId,
        defaultVideoStream = defaultVideoStream,
        audioStreams = audioStreams,
        subtitleStreams = subtitleStreams,
        subtitleFilePaths = subtitleFilePaths,
        priority = priority,
        addedAt = addedAt,
        quality = runCatching { DownloadQuality.valueOf(quality) }
            .getOrElse { DownloadQuality.FHD_1080 }
    )

    private fun Download.toEntity(): DownloadEntity = DownloadEntity(
        mediaId = mediaId,
        title = title,
        requestUri = requestUri,
        progress = progress.coerceIn(0f, 100f),
        status = status,
        type = type,
        downloadedBytes = downloadedBytes,
        filePath = filePath,
        checksum = checksum,
        mediaSourceId = mediaSourceId,
        defaultVideoStream = defaultVideoStream,
        audioStreams = audioStreams,
        subtitleStreams = subtitleStreams,
        subtitleFilePaths = subtitleFilePaths,
        priority = priority,
        addedAt = addedAt,
        quality = quality.name
    )
}
