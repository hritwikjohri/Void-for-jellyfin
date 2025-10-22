package com.hritwik.avoid.domain.model.playback

data class Segment(
    val id: String,
    val startPositionTicks: Long,
    val endPositionTicks: Long,
    val type: String
)