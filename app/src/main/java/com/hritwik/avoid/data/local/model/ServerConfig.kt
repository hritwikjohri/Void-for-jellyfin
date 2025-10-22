package com.hritwik.avoid.data.local.model

import kotlinx.serialization.Serializable

@Serializable
data class ServerConfig(
    val serverUrl: String,
    val serverName: String? = null,
    val version: String? = null,
    val isValid: Boolean = false
)