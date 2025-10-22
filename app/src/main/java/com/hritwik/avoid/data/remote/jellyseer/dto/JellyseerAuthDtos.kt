package com.hritwik.avoid.data.remote.jellyseer.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JellyseerJellyfinLoginRequestDto(
    val username: String,
    val password: String,
)

@Serializable
data class JellyseerUserDto(
    val id: Long? = null,
    val email: String? = null,
    val username: String? = null,
    @SerialName("displayName") val displayName: String? = null,
    @SerialName("plexUsername") val plexUsername: String? = null,
    @SerialName("jellyfinAuthToken") val jellyfinAuthToken: String? = null,
)

@Serializable
data class JellyseerStatusResponseDto(
    val status: String? = null,
)
