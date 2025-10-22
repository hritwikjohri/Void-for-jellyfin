package com.hritwik.avoid.domain.model.playback

data class PlaybackStopInfo(
    val itemId: String,
    val positionTicks: Long,
    val mediaSourceId: String? = null
)