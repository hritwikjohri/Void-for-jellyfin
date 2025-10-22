package com.hritwik.avoid.data.remote.jellyseer.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JellyseerCreateRequestDto(
    @SerialName("mediaType") val mediaType: String,
    @SerialName("mediaId") val mediaId: Long,
    @SerialName("tvdbId") val tvdbId: Long? = null,
    val seasons: List<Int>? = null,
    @SerialName("is4k") val is4k: Boolean = false,
    @SerialName("serverId") val serverId: Long? = null,
    @SerialName("profileId") val profileId: Long? = null,
    @SerialName("rootFolder") val rootFolder: String? = null,
    @SerialName("languageProfileId") val languageProfileId: Long? = null,
    @SerialName("userId") val userId: Long? = null
)
