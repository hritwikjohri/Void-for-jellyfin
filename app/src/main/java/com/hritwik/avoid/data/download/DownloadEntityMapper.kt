package com.hritwik.avoid.data.download

import android.net.Uri
import com.hritwik.avoid.data.local.database.entities.DownloadEntity
import com.hritwik.avoid.domain.model.download.DownloadCodec
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

fun DownloadEntity.toDownloadInfo(json: Json): DownloadService.DownloadInfo {
    val file = filePath?.let { File(it) } ?: File("")
    val dir = file.parentFile
    val source = mediaSourceId ?: mediaId
    val jsonFile = dir?.resolve("$source.json")
    val mediaItem = if (jsonFile?.exists() == true) {
        runCatching { json.decodeFromString<MediaItem>(jsonFile.readText()) }.getOrNull()
    } else null
    val finalItem = mediaItem ?: MediaItem(
        id = mediaId,
        name = title,
        title = title,
        type = type,
        overview = null,
        year = null,
        communityRating = null,
        runTimeTicks = null,
        primaryImageTag = null,
        thumbImageTag = null,
        logoImageTag = null,
        backdropImageTags = emptyList(),
        genres = emptyList(),
        isFolder = false,
        childCount = null,
        userData = null
    )
    val statusEnum = runCatching { DownloadService.DownloadStatus.valueOf(status) }
        .getOrElse { DownloadService.DownloadStatus.COMPLETED }
    val qualityEnum = runCatching { DownloadQuality.valueOf(quality) }
        .getOrElse { DownloadQuality.FHD_1080 }
    val parsedUri = runCatching { Uri.parse(requestUri) }.getOrNull()

    val codecParam = parsedUri?.getQueryParameter("VideoCodec")
    val codec = codecParam?.let { DownloadCodec.fromLabel(it) } ?: DownloadCodec.H264
    val request = DownloadRequest(
        quality = qualityEnum,
        static = parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true),
        maxWidth = parsedUri?.getQueryParameter("MaxWidth")?.toIntOrNull(),
        maxHeight = parsedUri?.getQueryParameter("MaxHeight")?.toIntOrNull(),
        maxBitrate = parsedUri?.getQueryParameter("MaxBitrate")?.toIntOrNull(),
        videoBitrate = parsedUri?.getQueryParameter("VideoBitrate")?.toIntOrNull(),
        audioBitrate = parsedUri?.getQueryParameter("AudioBitrate")?.toIntOrNull(),
        videoCodec = codec,
        audioCodec = parsedUri?.getQueryParameter("AudioCodec")
            ?: DownloadRequest.defaultAudioCodecs(),
        copySubtitles = parsedUri?.getQueryParameter("CopySubtitles").toBooleanStrictOrDefault(false),
        copyFontData = parsedUri?.getQueryParameter("CopyFonts").toBooleanStrictOrDefault(
            parsedUri?.getQueryParameter("EnableSubtitlesInManifest").toBooleanStrictOrDefault(false)
        ),
        enableAutoStreamCopy = parsedUri?.getQueryParameter("EnableAutoStreamCopy")
            .toBooleanStrictOrDefault(parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true)),
        allowVideoStreamCopy = parsedUri?.getQueryParameter("AllowVideoStreamCopy")
            .toBooleanStrictOrDefault(parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true)),
        allowAudioStreamCopy = parsedUri?.getQueryParameter("AllowAudioStreamCopy")
            .toBooleanStrictOrDefault(parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true)),
    )

    return DownloadService.DownloadInfo(
        mediaItem = finalItem,
        requestUri = requestUri,
        file = file,
        progress = progress,
        status = statusEnum,
        serverUrl = serverUrl,
        accessToken = accessToken,
        priority = priority,
        addedAt = addedAt,
        quality = qualityEnum,
        request = request,
        mediaSourceId = mediaSourceId,
        downloadedBytes = downloadedBytes,
        totalBytes = if (statusEnum == DownloadService.DownloadStatus.COMPLETED) {
            file.takeIf { it.exists() }?.length()?.takeIf { it > 0 }
        } else {
            null
        }
    )
}

private fun String?.toBooleanStrictOrDefault(default: Boolean): Boolean =
    this?.equals("true", ignoreCase = true) ?: default
