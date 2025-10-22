package com.hritwik.avoid.domain.model.auth

data class AuthSession(
    val userId: User,
    val server: Server,
    val accessToken: String,
    val isValid: Boolean = true
)