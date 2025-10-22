package com.hritwik.avoid.domain.model.playback

import com.hritwik.avoid.domain.model.library.MediaItem

data class PlaybackInfo(
    val mediaItem: MediaItem,
    val mediaSourceId: String? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null,
    val startPosition: Long = 0L,
    val videoQuality: String? = null,
    val maxBitrate: Int? = null
) {
    val hasCustomOptions: Boolean
        get() = mediaSourceId != null ||
                audioStreamIndex != null ||
                subtitleStreamIndex != null ||
                videoQuality != null ||
                maxBitrate != null

    val cacheKey: String
        get() = "${mediaItem.id}_${mediaSourceId ?: "default"}_${audioStreamIndex ?: -1}_${subtitleStreamIndex ?: -1}"
}