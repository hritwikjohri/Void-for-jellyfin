package com.hritwik.avoid.data.remote.dto.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDataDto(
    @SerialName("IsFavorite")
    val isFavorite: Boolean = false,
    @SerialName("PlaybackPositionTicks")
    val playbackPositionTicks: Long = 0,
    @SerialName("PlayCount")
    val playCount: Int = 0,
    @SerialName("Played")
    val played: Boolean = false,
    @SerialName("LastPlayedDate")
    val lastPlayedDate: String? = null
)