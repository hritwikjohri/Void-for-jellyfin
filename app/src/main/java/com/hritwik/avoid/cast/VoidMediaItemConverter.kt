package com.hritwik.avoid.cast

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaQueueItem

@UnstableApi
class VoidMediaItemConverter : MediaItemConverter {

    private val defaultConverter = DefaultMediaItemConverter()

    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val queueItem = defaultConverter.toMediaQueueItem(mediaItem)
        val durationMs =
            mediaItem.mediaMetadata.extras?.getLong(CastMediaHelper.EXTRA_DURATION_MS, 0L) ?: 0L

        if (durationMs > 0L) {
            val originalMedia = queueItem.media ?: return queueItem
            val updatedMedia = MediaInfo.Builder(originalMedia.contentId)
                .setContentType(originalMedia.contentType)
                .setContentUrl(originalMedia.contentUrl!!)
                .setStreamType(originalMedia.streamType)
                .setStreamDuration(durationMs)
                .setMetadata(originalMedia.metadata)
                .apply { originalMedia.customData?.let { setCustomData(it) } }
                .build()
            return MediaQueueItem.Builder(updatedMedia)
                .setStartTime(queueItem.startTime)
                .build()
        }

        return queueItem
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
        return defaultConverter.toMediaItem(mediaQueueItem)
    }
}
