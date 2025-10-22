package com.hritwik.avoid.domain.model.download

import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.download.DownloadQuality

data class Download(
    val mediaId: String,
    val requestUri: String,
    val title: String,
    val type: String,
    val progress: Float,
    val status: String,
    val downloadedBytes: Long = 0L,
    val filePath: String? = null,
    val checksum: String? = null,
    val mediaSourceId: String? = null,
    val defaultVideoStream: MediaStream? = null,
    val audioStreams: List<MediaStream> = emptyList(),
    val subtitleStreams: List<MediaStream> = emptyList(),
    val subtitleFilePaths: List<String> = emptyList(),
    val priority: Int = 0,
    val addedAt: Long = 0L,
    val quality: DownloadQuality = DownloadQuality.FHD_1080
)
