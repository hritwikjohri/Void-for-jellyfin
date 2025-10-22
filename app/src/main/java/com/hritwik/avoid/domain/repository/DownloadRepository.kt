package com.hritwik.avoid.domain.repository

import com.hritwik.avoid.domain.model.download.Download
import kotlinx.coroutines.flow.Flow

interface DownloadRepository {
    fun getDownloadsByStatus(status: String): Flow<List<Download>>
    suspend fun getDownload(mediaId: String): Download?
    fun getAllDownloads(): Flow<List<Download>>
    suspend fun insertDownload(download: Download)
    suspend fun updateDownload(download: Download)
    suspend fun deleteDownload(download: Download)
}
