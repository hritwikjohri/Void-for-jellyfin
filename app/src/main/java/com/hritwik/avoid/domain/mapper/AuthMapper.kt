package com.hritwik.avoid.domain.mapper

import com.hritwik.avoid.data.remote.dto.auth.AuthResponse
import com.hritwik.avoid.data.remote.dto.auth.ServerInfo
import com.hritwik.avoid.data.remote.dto.auth.UserInfo
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.model.auth.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthMapper @Inject constructor() {

    
    fun mapServerInfoToServer(dto: ServerInfo, serverUrl: String): Server {
        return Server(
            url = serverUrl,
            name = dto.serverName ?: "Jellyfin Server",
            version = dto.version ?: "Unknown",
            isConnected = true
        )
    }

    
    fun mapUserInfoToUser(dto: UserInfo): User {
        return User(
            id = dto.id,
            name = dto.name,
            hasPassword = dto.hasPassword
        )
    }

    
    fun mapAuthResponseToSession(
        dto: AuthResponse,
        server: Server
    ): AuthSession {
        val user = mapUserInfoToUser(dto.user)

        return AuthSession(
            userId = user,
            server = server,
            accessToken = dto.accessToken,
            isValid = true
        )
    }

    
    fun createServerFromPreferences(
        url: String,
        name: String?,
        version: String?,
        isConnected: Boolean = false
    ): Server {
        return Server(
            url = url,
            name = name ?: "Jellyfin Server",
            version = version ?: "Unknown",
            isConnected = isConnected
        )
    }

    
    fun createUserFromPreferences(
        id: String,
        name: String,
        hasPassword: Boolean = true
    ): User {
        return User(
            id = id,
            name = name,
            hasPassword = hasPassword
        )
    }

    
    fun createSessionFromPreferences(
        user: User,
        server: Server,
        accessToken: String
    ): AuthSession {
        return AuthSession(
            userId = user,
            server = server,
            accessToken = accessToken,
            isValid = true
        )
    }
}