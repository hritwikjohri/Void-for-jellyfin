package com.hritwik.avoid.domain.model.auth

data class LoginCredentials(
    val username: String,
    val password: String? = ""
)