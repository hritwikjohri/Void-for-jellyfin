package com.hritwik.avoid.domain.model.playback

data class PlaybackProgressInfo(
    val itemId: String,
    val positionTicks: Long,
    val isPaused: Boolean,
    val mediaSourceId: String? = null,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null
)
