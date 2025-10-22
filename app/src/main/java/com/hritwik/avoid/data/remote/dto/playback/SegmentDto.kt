package com.hritwik.avoid.data.remote.dto.playback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SegmentDto(
    @SerialName("Id") val id: String,
    @SerialName("StartTicks") val startTicks: Long,
    @SerialName("EndTicks") val endTicks: Long,
    @SerialName("Type") val type: String
)