package com.hritwik.avoid.domain.model.auth

data class Server(
    val url: String,
    val name: String,
    val version: String,
    val isConnected: Boolean = false,
    val connectionMethods: List<ServerConnectionMethod> = emptyList(),
    val activeConnection: ServerConnectionMethod? = null
)