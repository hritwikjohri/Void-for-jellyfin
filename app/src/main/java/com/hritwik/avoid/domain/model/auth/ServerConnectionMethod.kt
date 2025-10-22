package com.hritwik.avoid.domain.model.auth

import kotlinx.serialization.Serializable

@Serializable
data class ServerConnectionMethod(
    val url: String,
    val type: ServerConnectionType
) {
    val isLocal: Boolean
        get() = type == ServerConnectionType.LOCAL
}
