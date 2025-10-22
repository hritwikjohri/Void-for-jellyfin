package com.hritwik.avoid.data.remote.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class QuickConnectDto(
    @SerialName("Secret")
    val secret: String
)