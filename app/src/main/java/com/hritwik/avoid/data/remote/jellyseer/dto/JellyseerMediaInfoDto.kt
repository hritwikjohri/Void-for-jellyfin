package com.hritwik.avoid.data.remote.jellyseer.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JellyseerMediaInfoDto(
    val id: Long? = null,
    @SerialName("tmdbId") val tmdbId: Long? = null,
    @SerialName("tvdbId") val tvdbId: Long? = null,
    val status: Int? = null,
    val requests: List<JellyseerMediaRequestDto> = emptyList()
)

@Serializable
data class JellyseerMediaRequestDto(
    val id: Long,
    val status: Int,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("requestedBy") val requestedBy: JellyseerUserDto? = null
)

