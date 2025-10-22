package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    @SerialName("Username")
    val username: String,
    @SerialName("Pw")
    val password: String? = ""
)