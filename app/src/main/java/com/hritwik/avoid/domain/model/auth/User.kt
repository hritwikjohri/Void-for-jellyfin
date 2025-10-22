package com.hritwik.avoid.domain.model.auth

data class User(
    val id: String,
    val name: String,
    val hasPassword: Boolean
)