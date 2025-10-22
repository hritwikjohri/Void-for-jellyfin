package com.hritwik.avoid.domain.model.playback

import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream

data class PlaybackOptions(
    val selectedMediaSource: MediaSource? = null,
    val selectedVideoStream: MediaStream? = null,
    val selectedAudioStream: MediaStream? = null,
    val selectedSubtitleStream: MediaStream? = null,
    val startPositionTicks: Long? = null,
    val maxBitrate: Int? = null,
    val videoQuality: Int? = null,
    val enableDirectPlay: Boolean = true,
    val enableDirectStream: Boolean = true,
    val enableTranscoding: Boolean = true
)