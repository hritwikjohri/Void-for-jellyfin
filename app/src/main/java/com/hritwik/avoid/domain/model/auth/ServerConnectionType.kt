package com.hritwik.avoid.domain.model.auth

import java.net.URI
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class ServerConnectionType {
    LOCAL,
    REMOTE;

    companion object {
        fun fromUrl(url: String): ServerConnectionType {
            val host = runCatching { URI(url).host?.lowercase(Locale.US) }
                .getOrNull()
                ?.takeUnless { it.isBlank() }
                ?: return REMOTE

            if (host == "localhost" || host == "127.0.0.1" || host.endsWith(".local")) {
                return LOCAL
            }

            if (host.contains(':')) {
                val normalizedHost = host.removePrefix("[").removeSuffix("]")
                if (normalizedHost == "::1") {
                    return LOCAL
                }
                if (normalizedHost.startsWith("fe80")) {
                    return LOCAL
                }
                return REMOTE
            }

            val parts = host.split('.')
            if (parts.size == 4 && parts.all { it.toIntOrNull() != null }) {
                val octets = parts.mapNotNull { it.toIntOrNull() }
                if (octets.size == 4) {
                    return when {
                        octets[0] == 10 -> LOCAL
                        octets[0] == 192 && octets[1] == 168 -> LOCAL
                        octets[0] == 172 && octets[1] in 16..31 -> LOCAL
                        octets[0] == 169 && octets[1] == 254 -> LOCAL
                        else -> REMOTE
                    }
                }
            }

            return REMOTE
        }
    }
}
