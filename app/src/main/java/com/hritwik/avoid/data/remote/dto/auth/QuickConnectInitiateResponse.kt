package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuickConnectInitiateResponse(
    @SerialName("Code")
    val code: String,
    @SerialName("Secret")
    val secret: String
)