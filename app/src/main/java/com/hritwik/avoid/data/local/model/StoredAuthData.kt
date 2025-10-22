package com.hritwik.avoid.data.local.model

import kotlinx.serialization.Serializable

@Serializable
data class StoredAuthData(
    val serverUrl: String,
    val username: String,
    val accessToken: String,
    val userId: String,
    val serverId: String
)