package com.hritwik.avoid.domain.model.jellyseer

data class JellyseerConfig(
    val baseUrl: String = "",
    val apiKey: String = "",
    val sessionCookie: String = "",
    val userId: Long? = null,
    val userEmail: String? = null,
    val userDisplayName: String? = null,
) {
    val isConfigured: Boolean
        get() = baseUrl.isNotBlank() && (sessionCookie.isNotBlank() || apiKey.isNotBlank())

    val isLoggedIn: Boolean
        get() = sessionCookie.isNotBlank()
}
