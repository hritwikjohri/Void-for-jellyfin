package com.hritwik.avoid.data.remote.dto.playback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class PlaybackStartRequest(
    @SerialName("ItemId") val itemId: String,
    @SerialName("CanSeek") val canSeek: Boolean = true,
    @SerialName("PlaySessionId") val playSessionId: String? = null,
    @SerialName("MediaSourceId") val mediaSourceId: String? = null,
    @SerialName("PositionTicks") val positionTicks: Long? = null
)


@Serializable
data class PlaybackProgressRequest(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("IsPaused") val isPaused: Boolean = false,
    @SerialName("CanSeek") val canSeek: Boolean = true,
    @SerialName("PlaySessionId") val playSessionId: String? = null
)


@Serializable
data class PlaybackStopRequest(
    @SerialName("ItemId") val itemId: String,
    @SerialName("PositionTicks") val positionTicks: Long,
    @SerialName("PlaySessionId") val playSessionId: String? = null
)
