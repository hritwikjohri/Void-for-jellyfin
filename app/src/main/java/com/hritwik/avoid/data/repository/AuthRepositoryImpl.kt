package com.hritwik.avoid.data.repository

import android.content.Context
import com.hritwik.avoid.data.common.BaseRepository
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.data.remote.dto.auth.LoginRequest
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectDto
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectInitiateResponse
import com.hritwik.avoid.data.remote.dto.auth.QuickConnectResult
import com.hritwik.avoid.data.remote.dto.auth.UpdatePassword
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.LoginCredentials
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod
import com.hritwik.avoid.domain.model.auth.ServerConnectionType
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.utils.DataWiper
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import com.hritwik.avoid.utils.helpers.getDeviceName
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val retrofitBuilder: Retrofit.Builder,
    private val dataWiper: DataWiper,
    private val networkMonitor: NetworkMonitor,
    private val serverConnectionManager: ServerConnectionManager,
    @ApplicationContext private val context: Context,
    priorityDispatcher: PriorityDispatcher
) : BaseRepository(priorityDispatcher, serverConnectionManager), AuthRepository {

    private var currentApiService: JellyfinApiService? = null
    private var currentServerUrl: String? = null
    private val deviceId: String by lazy { getDeviceName(context) }

    private fun createApiService(serverUrl: String): JellyfinApiService {
        val baseUrl = if (!serverUrl.endsWith("/")) "$serverUrl/" else serverUrl
        return retrofitBuilder
            .baseUrl(baseUrl)
            .build()
            .create(JellyfinApiService::class.java)
    }

    private fun getApiServiceFor(serverUrl: String): JellyfinApiService {
        val normalizedUrl = serverConnectionManager.normalizeUrl(serverUrl)
        val existingService = currentApiService
        return if (existingService != null && currentServerUrl?.equals(normalizedUrl, ignoreCase = true) == true) {
            existingService
        } else {
            createApiService(normalizedUrl).also { apiService ->
                currentApiService = apiService
                currentServerUrl = normalizedUrl
            }
        }
    }

    override suspend fun connectToServer(serverUrl: String): NetworkResult<Server> {
        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }
        val normalizedUrl = serverConnectionManager.normalizeUrl(serverUrl)
        return safeApiCall(normalizedUrl) {
            val apiService = createApiService(normalizedUrl)
            val serverInfo = apiService.getServerInfo()
            currentServerUrl = normalizedUrl
            currentApiService = apiService
            serverConnectionManager.markRequestSuccess(normalizedUrl)
            val methods = preferencesManager.getServerConnections().first()
            val activeMethod = methods.firstOrNull { it.url.equals(normalizedUrl, ignoreCase = true) }
            val server = Server(
                url = normalizedUrl,
                name = serverInfo.serverName ?: "Server",
                version = serverInfo.version ?: "Unknown",
                isConnected = true,
                connectionMethods = methods,
                activeConnection = activeMethod
            )
            saveServerConfig(server)
            server
        }
    }

    override suspend fun authenticateUser(
        serverUrl: String,
        credentials: LoginCredentials
    ): NetworkResult<AuthSession> {
        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }
        val normalizedUrl = serverConnectionManager.normalizeUrl(serverUrl)
        return try {
            withTimeout(AUTH_TIMEOUT_MS) {
                enqueue(PriorityDispatcher.Priority.HIGH, normalizedUrl) {
                    val apiService = createApiService(normalizedUrl)
                    currentApiService = apiService
                    currentServerUrl = normalizedUrl

                    val loginRequest = LoginRequest(
                        username = credentials.username,
                        password = credentials.password
                    )

                    val authHeader = JellyfinApiService.createAuthHeader(deviceId)
                    val authResponse = apiService.authenticateByName(loginRequest, authHeader)
                    val serverInfo = try {
                        apiService.getServerInfo()
                    } catch (_: Exception) {
                        null
                    }

                    val user = User(
                        id = authResponse.user.id,
                        name = authResponse.user.name,
                        hasPassword = authResponse.user.hasPassword
                    )

                    val methods = preferencesManager.getServerConnections().first()
                    val activeMethod = methods.firstOrNull { it.url.equals(normalizedUrl, ignoreCase = true) }
                    val server = Server(
                        url = normalizedUrl,
                        name = serverInfo?.serverName ?: "Jellyfin Server",
                        version = serverInfo?.version ?: "Unknown",
                        isConnected = true,
                        connectionMethods = methods,
                        activeConnection = activeMethod
                    )

                    AuthSession(
                        userId = user,
                        server = server,
                        accessToken = authResponse.accessToken,
                        isValid = true
                    )
                }.await()
            }
        } catch (timeout: TimeoutCancellationException) {
            serverConnectionManager.markRequestFailure(normalizedUrl, AUTH_TIMEOUT_MESSAGE)
            NetworkResult.Error(AppError.Network(AUTH_TIMEOUT_MESSAGE), timeout)
        }
    }

    override suspend fun logout(): NetworkResult<Unit> {
        val accessToken = preferencesManager.getAccessToken().first()
        val storedUrl = preferencesManager.getServerUrl().first()
        val normalizedUrl = storedUrl?.let { serverConnectionManager.normalizeUrl(it) }

        val canCallServer = networkMonitor.isConnected.value && !accessToken.isNullOrBlank() && !normalizedUrl.isNullOrBlank()
        if (canCallServer) {
            val refreshed = serverConnectionManager.refreshActiveConnection(messageOnSwitch = null)
            val baseUrl = normalizedUrl
            val targetUrl = refreshed?.url ?: baseUrl
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val apiService = getApiServiceFor(targetUrl)
            val callResult = runCatching {
                withTimeout(LOGOUT_TIMEOUT_MS) {
                    apiService.logout(authHeader)
                }
            }
            if (callResult.isSuccess) {
                serverConnectionManager.markRequestSuccess(targetUrl)
            } else {
                val message = callResult.exceptionOrNull()?.message ?: "Unable to reach server for logout"
                serverConnectionManager.markRequestFailure(targetUrl, message)
            }
        }

        preferencesManager.clearAllPreferences()
        preferencesManager.clearRecentSearches()
        dataWiper.wipeAll()
        currentApiService = null
        currentServerUrl = null
        return NetworkResult.Success(Unit)
    }


    override suspend fun updatePassword(currentPassword: String, newPassword: String): NetworkResult<Unit> {
        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }
        val accessToken = preferencesManager.getAccessToken().first()
            ?: throw Exception("Access token missing")
        val storedUrl = preferencesManager.getServerUrl().first()
            ?: throw Exception("Server URL missing")
        val normalizedUrl = serverConnectionManager.normalizeUrl(storedUrl)
        val userId = preferencesManager.getUserId().first()
            ?: throw Exception("User ID missing")

        return safeApiCall(normalizedUrl) {
            val apiService = getApiServiceFor(normalizedUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            val request = UpdatePassword(
                currentPw = currentPassword,
                newPw = newPassword,
                resetPassword = false
            )
            apiService.updatePassword(userId, request, authHeader)
        }
    }

    override suspend fun initiateQuickConnect(serverUrl: String): NetworkResult<QuickConnectInitiateResponse> {
        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }
        val normalizedUrl = serverConnectionManager.normalizeUrl(serverUrl)
        return safeApiCall(normalizedUrl) {
            val apiService = getApiServiceFor(normalizedUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId)
            apiService.initiateQuickConnect(authHeader)
        }
    }

    override suspend fun pollQuickConnect(secret: String): NetworkResult<QuickConnectResult> {
        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }
        val targetUrl = serverConnectionManager.state.value.activeMethod?.url
            ?: currentServerUrl
            ?: throw Exception("Server URL not set")
        val normalizedUrl = serverConnectionManager.normalizeUrl(targetUrl)
        return safeApiCall(normalizedUrl) {
            val apiService = getApiServiceFor(normalizedUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId)
            apiService.connectQuickConnect(secret, authHeader)
        }
    }

    override suspend fun authorizeQuickConnect(secret: String): NetworkResult<AuthSession> {
        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }

        val targetUrl = serverConnectionManager.state.value.activeMethod?.url
            ?: currentServerUrl
            ?: throw Exception("Server URL not set")
        val normalizedUrl = serverConnectionManager.normalizeUrl(targetUrl)
        val result = safeApiCall(normalizedUrl) {
            val apiService = getApiServiceFor(normalizedUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId)

            val quickConnectRequest = QuickConnectDto(secret = secret)
            val authResponse = apiService.authorizeQuickConnect(quickConnectRequest, authHeader)
            val serverInfo = try {
                apiService.getServerInfo()
            } catch (_: Exception) {
                null
            }
            val user = User(
                id = authResponse.user.id,
                name = authResponse.user.name,
                hasPassword = authResponse.user.hasPassword
            )

            val methods = preferencesManager.getServerConnections().first()
            val activeMethod = methods.firstOrNull { it.url.equals(normalizedUrl, ignoreCase = true) }
            val server = Server(
                url = normalizedUrl,
                name = serverInfo?.serverName ?: "Server",
                version = serverInfo?.version ?: "Unknown",
                isConnected = true,
                connectionMethods = methods,
                activeConnection = activeMethod
            )

            AuthSession(
                userId = user,
                server = server,
                accessToken = authResponse.accessToken,
                isValid = true
            )
        }

        return when (result) {
            is NetworkResult.Error -> {
                val pendingAuthorizationMessage = "Quick Connect authorization pending"
                if (result.message.equals("Server not found", ignoreCase = true)) {
                    NetworkResult.Error(AppError.Validation(pendingAuthorizationMessage))
                } else {
                    result
                }
            }

            else -> result
        }
    }

    override suspend fun isQuickConnectEnabled(serverUrl: String): NetworkResult<Boolean> {
        val normalizedUrl = serverConnectionManager.normalizeUrl(serverUrl)
        return safeApiCall(normalizedUrl) {
            val apiService = getApiServiceFor(normalizedUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId)
            apiService.isQuickConnectEnabled(authHeader)
        }
    }

    override suspend fun saveAuthSession(session: AuthSession): NetworkResult<Unit> {
        val normalizedUrl = serverConnectionManager.normalizeUrl(session.server.url)
        return safeApiCall {
        preferencesManager.saveServerConfig(
                normalizedUrl,
                session.server.name
            )
            preferencesManager.saveServerDetails(
                serverVersion = session.server.version,
                serverConnected = session.server.isConnected
            )
            preferencesManager.saveAuthData(
                username = session.userId.name,
                accessToken = session.accessToken,
                userId = session.userId.id,
                serverId = "default" 
            )
        }
    }

    override suspend fun getSavedAuthSession(): NetworkResult<AuthSession?> {
        return safeApiCall {
            val serverUrl = preferencesManager.getServerUrl().first()
            val serverName = preferencesManager.getServerName().first()
            val serverVersion = preferencesManager.getServerVersion().first()
            val serverConnected = preferencesManager.getServerConnected().first()
            val username = preferencesManager.getUsername().first()
            val accessToken = preferencesManager.getAccessToken().first()
            val userId = preferencesManager.getUserId().first()

            if (serverUrl != null && username != null && accessToken != null && userId != null) {
                val normalizedUrl = serverConnectionManager.normalizeUrl(serverUrl)
                getApiServiceFor(normalizedUrl)
                serverConnectionManager.markRequestSuccess(normalizedUrl)

                val user = User(
                    id = userId,
                    name = username,
                    hasPassword = true
                )

                val methods = preferencesManager.getServerConnections().first()
                val activeMethod = methods.firstOrNull { it.url.equals(normalizedUrl, ignoreCase = true) }
                val server = Server(
                    url = normalizedUrl,
                    name = serverName ?: "Server",
                    version = serverVersion ?: "Unknown",
                    isConnected = serverConnected,
                    connectionMethods = methods,
                    activeConnection = activeMethod
                )

                val session = AuthSession(
                    userId = user,
                    server = server,
                    accessToken = accessToken,
                    isValid = true
                )
                session
            } else {
                null
            }
        }
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return preferencesManager.isLoggedIn()
    }

    override suspend fun clearAuthData(): NetworkResult<Unit> {
        val accessToken = preferencesManager.getAccessToken().first()
        val storedUrl = preferencesManager.getServerUrl().first()
            ?: throw Exception("Server URL missing")
        val normalizedUrl = serverConnectionManager.normalizeUrl(storedUrl)
        return safeApiCall(normalizedUrl) {
            accessToken?.let { closeServerSession(it, normalizedUrl) }
            preferencesManager.clearAuthData()
            currentApiService = null
            currentServerUrl = null
        }
    }

    override suspend fun saveServerConfig(server: Server): NetworkResult<Unit> {
        val normalizedUrl = serverConnectionManager.normalizeUrl(server.url)
        return safeApiCall(normalizedUrl) {
            serverConnectionManager.markRequestSuccess(normalizedUrl)
            preferencesManager.saveServerConfig(normalizedUrl, server.name)
            if (server.connectionMethods.isNotEmpty()) {
                val normalizedConnections = server.connectionMethods
                    .map { method ->
                        val normalized = serverConnectionManager.normalizeUrl(method.url)
                        ServerConnectionMethod(normalized, ServerConnectionType.fromUrl(normalized))
                    }
                    .distinctBy { it.url.lowercase() }
                preferencesManager.saveServerConnections(normalizedConnections)
            }
            currentServerUrl = normalizedUrl
        }
    }

    override suspend fun getSavedServerConfig(): NetworkResult<Server?> {
        return safeApiCall {
            val serverUrl = preferencesManager.getServerUrl().first()
            val serverName = preferencesManager.getServerName().first()
            val serverVersion = preferencesManager.getServerVersion().first()
            val methods = preferencesManager.getServerConnections().first()

            if (serverUrl != null) {
                val normalizedUrl = serverConnectionManager.normalizeUrl(serverUrl)
                val activeMethod = methods.firstOrNull { it.url.equals(normalizedUrl, ignoreCase = true) }
                val server = Server(
                    url = normalizedUrl,
                    name = serverName ?: "Server",
                    version = serverVersion ?: "Unknown",
                    isConnected = false,
                    connectionMethods = methods,
                    activeConnection = activeMethod
                )

                println("📱 Retrieved saved server config: ${server.name} v${server.version}")
                server
            } else {
                println("📱 No saved server config found")
                null
            }
        }
    }

    override suspend fun validateSession(): NetworkResult<Boolean> {
        val accessToken = preferencesManager.getAccessToken().first()
        val storedUrl = preferencesManager.getServerUrl().first()
        val normalizedUrl = storedUrl?.let { serverConnectionManager.normalizeUrl(it) }

        if (accessToken.isNullOrBlank() || normalizedUrl.isNullOrBlank()) {
            return NetworkResult.Success(false)
        }

        if (!networkMonitor.isConnected.value) {
            return NetworkResult.Error(AppError.Network("No network connection"))
        }

        val connectionState = serverConnectionManager.state.value
        if (connectionState.isOffline) {
            val message = connectionState.lastMessage ?: "Offline mode enabled"
            return NetworkResult.Error(AppError.Network(message))
        }

        val targetUrl = normalizedUrl
        val apiService = getApiServiceFor(targetUrl)

        return safeApiCall(targetUrl) {
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            apiService.getCurrentUser(authHeader)
            true
        }
    }

    private suspend fun closeServerSession(accessToken: String, serverUrl: String) {
        val apiService = getApiServiceFor(serverUrl)
        val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
        apiService.logout(authHeader)
    }

    companion object {
        private const val LOGOUT_TIMEOUT_MS = 5_000L
        private const val AUTH_TIMEOUT_MS = 5_000L
        private const val AUTH_TIMEOUT_MESSAGE = "Login timed out. Please try again."
    }
}