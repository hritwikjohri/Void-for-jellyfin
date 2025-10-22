package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthResponse(
    @SerialName("User")
    val user: UserInfo,
    @SerialName("AccessToken")
    val accessToken: String,
    @SerialName("ServerId")
    val serverId: String
)