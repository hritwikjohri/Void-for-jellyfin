package com.hritwik.avoid.domain.repository

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectInitiateResponse
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectResult
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.LoginCredentials
import com.hritwik.avoid.domain.model.auth.Server
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun connectToServer(serverUrl: String): NetworkResult<Server>

    suspend fun authenticateUser(
        serverUrl: String,
        credentials: LoginCredentials
    ): NetworkResult<AuthSession>

    suspend fun logout(): NetworkResult<Unit>

    suspend fun saveAuthSession(session: AuthSession): NetworkResult<Unit>

    suspend fun getSavedAuthSession(): NetworkResult<AuthSession?>

    fun isLoggedIn(): Flow<Boolean>

    suspend fun clearAuthData(): NetworkResult<Unit>

    suspend fun validateSession(): NetworkResult<Boolean>

    suspend fun saveServerConfig(server: Server): NetworkResult<Unit>

    suspend fun getSavedServerConfig(): NetworkResult<Server?>

    suspend fun updatePassword(currentPassword: String, newPassword: String): NetworkResult<Unit>

    suspend fun initiateQuickConnect(serverUrl: String): NetworkResult<QuickConnectInitiateResponse>

    suspend fun pollQuickConnect(secret: String): NetworkResult<QuickConnectResult>

    suspend fun authorizeQuickConnect(secret: String): NetworkResult<AuthSession>

    suspend fun isQuickConnectEnabled(serverUrl: String): NetworkResult<Boolean>
}