package com.hritwik.avoid.cast

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.constants.ApiConstants
import androidx.core.net.toUri

object CastMediaHelper {

    const val EXTRA_DURATION_MS = "durationMs"
    fun buildCastMediaItem(
        mediaItem: MediaItem,
        videoUrl: String,
        serverUrl: String,
        startPositionMs: Long = 0
    ): ExoMediaItem {
        val artworkUrl = mediaItem.primaryImageTag?.let { tag ->
            val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
            "${baseUrl}Items/${mediaItem.id}/Images/Primary?tag=$tag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=480"
        }

        val durationMs = mediaItem.runTimeTicks?.let { it / 10_000 } ?: 0L

        val extras = Bundle().apply {
            putLong(EXTRA_DURATION_MS, durationMs)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle(mediaItem.name)
            .setSubtitle(mediaItem.seriesName)
            .setArtworkUri(artworkUrl?.toUri())
            .setExtras(extras)
            .build()

        return ExoMediaItem.Builder()
            .setUri(videoUrl)
            .setMediaId(mediaItem.id)
            .setMediaMetadata(metadata)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
    }

    fun buildCastStreamUrl(
        serverUrl: String,
        itemId: String,
        mediaSourceId: String?,
        accessToken: String,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): String {
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return buildString {
            append("${baseUrl}Videos/$itemId/stream.mp4")
            append("?Static=true")
            append("&MediaSourceId=${mediaSourceId ?: itemId}")
            append("&api_key=$accessToken")
            audioStreamIndex?.let { append("&AudioStreamIndex=$it") }
            subtitleStreamIndex?.let { append("&SubtitleStreamIndex=$it") }
            append("&AudioCodec=aac")
            append("&VideoCodec=h264")
            append("&Container=mp4")
            append("&EnableAutoStreamCopy=true")
            append("&AllowVideoStreamCopy=true")
            append("&AllowAudioStreamCopy=true")
        }
    }
}
