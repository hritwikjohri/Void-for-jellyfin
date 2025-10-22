package com.hritwik.avoid.data.connection

import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod

data class ServerConnectionState(
    val methods: List<ServerConnectionMethod> = emptyList(),
    val activeMethod: ServerConnectionMethod? = null,
    val isOffline: Boolean = false,
    val isWifiConnected: Boolean = false,
    val lastMessage: String? = null
)
