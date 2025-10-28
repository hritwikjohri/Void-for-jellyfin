package com.hritwik.avoid.presentation.viewmodel.auth

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.connection.ServerConnectionEvent
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.presentation.ui.state.AuthServerState
import com.hritwik.avoid.presentation.ui.state.InitializationState
import com.hritwik.avoid.presentation.ui.state.PasswordChangeState
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.auth.LoginCredentials
import com.hritwik.avoid.domain.provider.AuthSessionProvider
import com.hritwik.avoid.domain.usecase.auth.AuthenticateUserUseCase
import com.hritwik.avoid.domain.usecase.auth.AuthorizeQuickConnectUseCase
import com.hritwik.avoid.domain.usecase.auth.ChangePasswordUseCase
import com.hritwik.avoid.domain.usecase.auth.GetSavedAuthUseCase
import com.hritwik.avoid.domain.usecase.auth.LogoutUseCase
import com.hritwik.avoid.domain.usecase.auth.ClearAuthDataUseCase
import com.hritwik.avoid.domain.usecase.auth.ConnectToServerUseCase
import com.hritwik.avoid.domain.usecase.auth.GetSavedServerUseCase
import com.hritwik.avoid.domain.usecase.auth.InitiateQuickConnectUseCase
import com.hritwik.avoid.domain.usecase.auth.PollQuickConnectUseCase
import com.hritwik.avoid.domain.usecase.auth.SaveServerConfigUseCase
import com.hritwik.avoid.domain.usecase.auth.ValidateSessionUseCase
import com.hritwik.avoid.presentation.ui.state.QuickConnectState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@HiltViewModel
class AuthServerViewModel @Inject constructor(
    private val authenticateUserUseCase: AuthenticateUserUseCase,
    private val getSavedAuthUseCase: GetSavedAuthUseCase,
    private val validateSessionUseCase: ValidateSessionUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val clearAuthDataUseCase: ClearAuthDataUseCase,
    private val authSessionProvider: AuthSessionProvider,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val saveServerConfigUseCase: SaveServerConfigUseCase,
    private val getSavedServerUseCase: GetSavedServerUseCase,
    private val authorizeQuickConnectUseCase: AuthorizeQuickConnectUseCase,
    private val initiateQuickConnectUseCase: InitiateQuickConnectUseCase,
    private val pollQuickConnectUseCase: PollQuickConnectUseCase,
    private val serverConnectionManager: ServerConnectionManager,
    private val preferencesManager: PreferencesManager,
    private val okHttpClient: OkHttpClient
) : ViewModel() {

    private val _state = MutableStateFlow(AuthServerState())
    val state: StateFlow<AuthServerState> = _state.asStateFlow()
    val connectionEvents: SharedFlow<ServerConnectionEvent> = serverConnectionManager.events

    private val _passwordChangeState = MutableStateFlow<PasswordChangeState>(PasswordChangeState.Idle)
    val passwordChangeState: StateFlow<PasswordChangeState> = _passwordChangeState.asStateFlow()

    private var quickConnectJob: Job? = null

    init {
        state.onEach { authSessionProvider.updateSession(it.authSession) }.launchIn(viewModelScope)
        viewModelScope.launch {
            serverConnectionManager.state.collect { connectionState ->
                val current = _state.value
                val updatedServer = current.server?.let { server ->
                    server.copy(
                        url = connectionState.activeMethod?.url ?: server.url,
                        connectionMethods = connectionState.methods,
                        activeConnection = connectionState.activeMethod
                    )
                }
                val updatedAuthSession = current.authSession?.let { session ->
                    val serverForSession = updatedServer ?: session.server.copy(
                        url = connectionState.activeMethod?.url ?: session.server.url,
                        connectionMethods = connectionState.methods,
                        activeConnection = connectionState.activeMethod
                    )
                    session.copy(server = serverForSession)
                }
                _state.value = current.copy(
                    connectionMethods = connectionState.methods,
                    activeConnectionMethod = connectionState.activeMethod,
                    isOfflineMode = connectionState.isOffline,
                    isWifiConnected = connectionState.isWifiConnected,
                    localConnectionUrls = connectionState.methods.filter { it.isLocal }.map { it.url },
                    remoteConnectionUrls = connectionState.methods.filterNot { it.isLocal }.map { it.url },
                    serverUrl = connectionState.activeMethod?.url ?: updatedServer?.url ?: current.serverUrl,
                    server = updatedServer,
                    authSession = updatedAuthSession
                )
            }
        }
        viewModelScope.launch {
            serverConnectionManager.ensureActiveConnection()
        }
        viewModelScope.launch {
            combine(
                preferencesManager.isMtlsEnabled(),
                preferencesManager.getMtlsCertificateName(),
                preferencesManager.getMtlsCertificatePassword()
            ) { enabled, certificateName, password ->
                Triple(enabled, certificateName, password)
            }.collect { (enabled, certificateName, password) ->
                _state.value = _state.value.copy(
                    isMtlsEnabled = enabled,
                    mtlsCertificateName = certificateName,
                    mtlsCertificatePassword = password.orEmpty()
                )
            }
        }
        loadSavedServer()
        checkSavedAuth()
    }

    fun saveLocalConnections(urls: List<String>) {
        viewModelScope.launch {
            serverConnectionManager.setLocalAddresses(urls)
        }
    }

    fun clearLocalConnections() {
        viewModelScope.launch {
            serverConnectionManager.setLocalAddresses(emptyList())
        }
    }

    fun saveRemoteConnections(urls: List<String>) {
        viewModelScope.launch {
            serverConnectionManager.setRemoteAddresses(urls)
        }
    }

    suspend fun resetServerConfiguration() {
        serverConnectionManager.clearConnectionState()
        preferencesManager.clearServerConfiguration()
        resetAuthState()
    }

    fun clearRemoteConnections() {
        viewModelScope.launch {
            serverConnectionManager.setRemoteAddresses(emptyList())
        }
    }

    fun retryConnectionEvaluation() {
        viewModelScope.launch {
            serverConnectionManager.refreshActiveConnection(messageOnSwitch = "Rechecked server connection")
        }
    }

    fun updateServerPushAddress(address: String) {
        _state.value = _state.value.copy(
            serverPushAddress = address,
            serverPushFeedback = null,
            serverPushSuccess = null
        )
    }

    fun sendServerDetails(context: Context) {
        val currentState = _state.value
        val endpoint = currentState.serverPushAddress.trim()
        if (endpoint.isEmpty()) {
            _state.value = currentState.copy(
                serverPushFeedback = "Enter a destination IP and port",
                serverPushSuccess = false
            )
            return
        }

        val serverUrl = currentState.activeConnectionMethod?.url
            ?: currentState.serverUrl
            ?: currentState.server?.url

        if (serverUrl.isNullOrBlank()) {
            _state.value = currentState.copy(
                serverPushFeedback = "No active server connection available",
                serverPushSuccess = false
            )
            return
        }

        val endpointUrl = if (endpoint.startsWith("http://", ignoreCase = true) ||
            endpoint.startsWith("https://", ignoreCase = true)
        ) {
            endpoint
        } else {
            "http://$endpoint"
        }

        val httpUrl = endpointUrl.toHttpUrlOrNull()
        if (httpUrl == null) {
            _state.value = currentState.copy(
                serverPushFeedback = "Invalid destination address",
                serverPushSuccess = false
            )
            return
        }

        viewModelScope.launch {
            _state.value = _state.value.copy(
                isServerPushInProgress = true,
                serverPushFeedback = null,
                serverPushSuccess = null
            )

            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val parsedServerUri = runCatching { Uri.parse(serverUrl) }.getOrNull()
                    val scheme = parsedServerUri?.scheme?.lowercase(Locale.US) ?: "http"
                    val connectionType = if (scheme == "https") "HTTPS" else "HTTP"
                    val port = parsedServerUri?.port?.takeIf { it != -1 }
                        ?: if (connectionType == "HTTPS") 443 else 80
                    val host = parsedServerUri?.host?.takeUnless { it.isNullOrBlank() } ?: serverUrl
                    val fullUrl = currentState.activeConnectionMethod?.url
                        ?: parsedServerUri?.toString()
                        ?: serverUrl

                    val token = currentState.authSession?.accessToken
                        ?: preferencesManager.getAccessToken().firstOrNull().orEmpty()

                    val isMtlsEnabledForPush = currentState.serverPushFileUri != null
                    val payload = ServerPushPayload(
                        serverUrl = host,
                        port = port,
                        connectionType = connectionType,
                        fullUrl = fullUrl,
                        mediaToken = token,
                        mTLS = isMtlsEnabledForPush,
                        password = if (isMtlsEnabledForPush) currentState.serverPushPassword else ""
                    )

                    val jsonBody = Json.encodeToString(payload)
                    val multipartBuilder = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart(
                            name = "serverDetails",
                            filename = "server_details.json",
                            body = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType())
                        )
                        .addFormDataPart("destination", endpointUrl)

                    currentState.serverPushFileUri?.let { fileUri ->
                        val fileName = currentState.serverPushFileName
                            ?: getDisplayName(context, fileUri)
                            ?: fileUri.lastPathSegment
                            ?: "upload.bin"
                        val mimeType = context.contentResolver.getType(fileUri)?.takeIf { it.isNotBlank() }
                            ?: "application/octet-stream"
                        val fileBytes = context.contentResolver.openInputStream(fileUri)?.use { input ->
                            input.readBytes()
                        } ?: throw IllegalStateException("Unable to read selected file")
                        val mediaType = mimeType.toMediaTypeOrNull() ?: "application/octet-stream".toMediaType()
                        multipartBuilder.addFormDataPart(
                            name = "file",
                            filename = fileName,
                            body = fileBytes.toRequestBody(mediaType)
                        )
                    }

                    val requestBody = multipartBuilder.build()
                    val request = Request.Builder()
                        .url(httpUrl)
                        .post(requestBody)
                        .build()

                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw IOException("Unexpected response code ${'$'}{response.code}")
                        }
                    }
                }
            }

            _state.value = _state.value.copy(
                isServerPushInProgress = false,
                serverPushFeedback = result.fold(
                    onSuccess = { "Server details sent successfully" },
                    onFailure = { throwable ->
                        "Failed to send server details"
                    }
                ),
                serverPushSuccess = result.isSuccess
            )
        }
    }

    fun selectServerPushFile(context: Context, uri: Uri?) {
        if (uri == null) {
            _state.value = _state.value.copy(
                serverPushFileName = null,
                serverPushFileUri = null,
                serverPushPassword = ""
            )
            return
        }

        val displayName = getDisplayName(context, uri) ?: uri.lastPathSegment ?: "upload.bin"
        _state.value = _state.value.copy(
            serverPushFileName = displayName,
            serverPushFileUri = uri,
            serverPushFeedback = null,
            serverPushSuccess = null,
            serverPushPassword = ""
        )
    }

    fun clearServerPushFile() {
        _state.value = _state.value.copy(
            serverPushFileName = null,
            serverPushFileUri = null,
            serverPushFeedback = null,
            serverPushSuccess = null,
            serverPushPassword = ""
        )
    }

    fun updateServerPushPassword(password: String) {
        _state.value = _state.value.copy(
            serverPushPassword = password,
            serverPushFeedback = null,
            serverPushSuccess = null
        )
    }

    fun setMtlsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.setMtlsEnabled(enabled)
            if (!enabled) {
                _state.value = _state.value.copy(mtlsError = null)
            }
        }
    }

    fun clearMtlsError() {
        _state.value = _state.value.copy(mtlsError = null)
    }

    fun importMtlsCertificate(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMtlsImporting = true, mtlsError = null)
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        input.readBytes()
                    } ?: throw IllegalStateException("Unable to open selected certificate")
                }
                if (bytes.isEmpty()) {
                    throw IllegalArgumentException("Selected certificate is empty")
                }
                val displayName = getDisplayName(context, uri) ?: uri.lastPathSegment ?: "mtls_certificate"
                preferencesManager.saveMtlsCertificate(bytes, displayName)
                preferencesManager.setMtlsEnabled(true)
                preferencesManager.setMtlsCertificatePassword("")
                _state.value = _state.value.copy(
                    isMtlsImporting = false,
                    mtlsError = null,
                    isMtlsEnabled = true,
                    mtlsCertificateName = displayName,
                    mtlsCertificatePassword = ""
                )
            } catch (error: Exception) {
                _state.value = _state.value.copy(
                    isMtlsImporting = false,
                    mtlsError = error.message ?: "Failed to import certificate"
                )
            }
        }
    }

    fun updateMtlsCertificatePassword(password: String) {
        _state.value = _state.value.copy(mtlsCertificatePassword = password)
        viewModelScope.launch {
            preferencesManager.setMtlsCertificatePassword(password)
        }
    }

    fun removeMtlsCertificate() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isMtlsImporting = true, mtlsError = null)
            runCatching {
                preferencesManager.clearMtlsCertificate()
                preferencesManager.setMtlsEnabled(false)
            }.onSuccess {
                _state.value = _state.value.copy(
                    isMtlsImporting = false,
                    mtlsCertificateName = null,
                    mtlsCertificatePassword = "",
                    isMtlsEnabled = false
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    isMtlsImporting = false,
                    mtlsError = error.message ?: "Failed to remove certificate"
                )
            }
        }
    }

    fun connectToServer(serverUrl: String) {
        viewModelScope.launch {
            if (_state.value.isMtlsEnabled && _state.value.mtlsCertificateName.isNullOrBlank()) {
                _state.value = _state.value.copy(
                    mtlsError = "Upload an mTLS certificate before connecting.",
                    isLoading = false,
                    isConnected = false
                )
                return@launch
            }
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                isConnected = false,
                mtlsError = null
            )

            val cleanUrl = cleanServerUrl(serverUrl)

            when (val result = connectToServerUseCase(cleanUrl)) {
                is NetworkResult.Success -> {
                    saveServerConfigUseCase(result.data)
                    val connectionState = serverConnectionManager.state.value
                    val updatedServer = result.data.copy(
                        connectionMethods = connectionState.methods,
                        activeConnection = connectionState.activeMethod,
                        url = connectionState.activeMethod?.url ?: result.data.url
                    )
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isConnected = true,
                        server = updatedServer,
                        serverUrl = updatedServer.url,
                        connectionMethods = connectionState.methods,
                        activeConnectionMethod = connectionState.activeMethod,
                        isOfflineMode = connectionState.isOffline,
                        isWifiConnected = connectionState.isWifiConnected,
                        localConnectionUrls = connectionState.methods.filter { it.isLocal }.map { it.url },
                        remoteConnectionUrls = connectionState.methods.filterNot { it.isLocal }.map { it.url },
                        error = null
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isConnected = false,
                        server = null,
                        error = result.message
                    )
                }
                else -> Unit
            }
        }
    }

    fun login(username: String, password: String) {
        val url = _state.value.server?.url ?: _state.value.serverUrl
        if (url.isNullOrBlank()) {
            _state.value = _state.value.copy(error = "Server URL not set")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val credentials = LoginCredentials(username, password)
            val params = AuthenticateUserUseCase.Params(url, credentials)
            when (val result = authenticateUserUseCase(params)) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = true,
                        authSession = result.data,
                        error = null
                    )
                }
                is NetworkResult.Error -> {
                    val errorMessage = if (result.message.contains("timed out", ignoreCase = true)) {
                        LOGIN_TIMEOUT_MESSAGE
                    } else {
                        result.message
                    }
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        error = errorMessage
                    )
                }
                else -> Unit
            }
        }
    }

    fun initiateQuickConnect() {
        val url = _state.value.server?.url ?: _state.value.serverUrl
        if (url.isNullOrBlank()) {
            _state.value = _state.value.copy(
                quickConnectState = QuickConnectState(error = "Server URL not set")
            )
            return
        }
        viewModelScope.launch {
            when (val result = initiateQuickConnectUseCase(InitiateQuickConnectUseCase.Params(url))) {
                is NetworkResult.Success -> {
                    _state.value = _state.value.copy(
                        quickConnectState = QuickConnectState(
                            code = result.data.code,
                            secret = result.data.secret,
                            isPolling = false,
                            error = null
                        )
                    )
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        quickConnectState = QuickConnectState(error = result.message)
                    )
                }
                else -> Unit
            }
        }
    }

    fun pollQuickConnect(timeoutMillis: Long = 60000L) {
        val secret = _state.value.quickConnectState.secret ?: return
        quickConnectJob?.cancel()
        quickConnectJob = viewModelScope.launch {
            _state.value = _state.value.copy(
                quickConnectState = _state.value.quickConnectState.copy(isPolling = true, error = null)
            )
            val startTime = System.currentTimeMillis()
            val fallbackAuthorizeDelay = 10_000L
            while (System.currentTimeMillis() - startTime < timeoutMillis) {
                when (val stateResult = pollQuickConnectUseCase(PollQuickConnectUseCase.Params(secret))) {
                    is NetworkResult.Success -> {
                        val elapsed = System.currentTimeMillis() - startTime
                        val shouldAttemptAuthorization =
                            stateResult.data.authenticated || elapsed >= fallbackAuthorizeDelay

                        if (shouldAttemptAuthorization) {
                            when (val authResult = authorizeQuickConnectUseCase(AuthorizeQuickConnectUseCase.Params(secret))) {
                                is NetworkResult.Success -> {
                                    _state.value = _state.value.copy(
                                        isAuthenticated = true,
                                        authSession = authResult.data,
                                        quickConnectState = QuickConnectState()
                                    )
                                    return@launch
                                }
                                is NetworkResult.Error -> {
                                    val message = authResult.message
                                    val isPending = message.equals("Quick Connect authorization pending", ignoreCase = true) ||
                                        message.equals("Server not found", ignoreCase = true)

                                    if (!isPending) {
                                        _state.value = _state.value.copy(
                                            quickConnectState = _state.value.quickConnectState.copy(
                                                isPolling = false,
                                                error = message
                                            )
                                        )
                                        return@launch
                                    }
                                }
                                else -> Unit
                            }
                        }
                        delay(2000)
                    }
                    is NetworkResult.Error -> {
                        delay(2000)
                    }
                    else -> Unit
                }
            }
            _state.value = _state.value.copy(
                quickConnectState = _state.value.quickConnectState.copy(
                    isPolling = false,
                    error = "Quick Connect timed out",
                )
            )
        }
    }

    fun resetQuickConnectState() {
        quickConnectJob?.cancel()
        quickConnectJob = null
        _state.value = _state.value.copy(quickConnectState = QuickConnectState())
    }

    fun logout() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            when (logoutUseCase()) {
                is NetworkResult.Success -> {
                    resetAuthState()
                }
                is NetworkResult.Error -> {
                    clearLocalAuthData()
                }
                is NetworkResult.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
        }
    }

    fun updatePassword(current: String, new: String) {
        viewModelScope.launch {
            val params = ChangePasswordUseCase.Params(current, new)
            _passwordChangeState.value = PasswordChangeState.Loading
            when (val result = changePasswordUseCase(params)) {
                is NetworkResult.Success -> {
                    _passwordChangeState.value = PasswordChangeState.Success
                }
                is NetworkResult.Error -> {
                    _passwordChangeState.value = PasswordChangeState.Error(result.message)
                }
                else -> Unit
            }
        }
    }

    fun switchUser() {
        viewModelScope.launch {
            clearAuthDataUseCase()
            _state.value = _state.value.copy(
                isAuthenticated = false,
                authSession = null
            )
        }
    }

    private suspend fun clearLocalAuthData() {
        clearAuthDataUseCase()
        resetAuthState()
    }

    fun resetAuthState() {
        _state.value = AuthServerState(initializationState = InitializationState.Initialized)
    }

    private fun checkSavedAuth() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = getSavedAuthUseCase()) {
                is NetworkResult.Success -> {
                    val savedSession = result.data
                    if (savedSession != null) {
                        val connectionState = serverConnectionManager.state.value
                        val resolvedMethods = connectionState.methods.ifEmpty { savedSession.server.connectionMethods }
                        val updatedServer = savedSession.server.copy(
                            connectionMethods = resolvedMethods,
                            activeConnection = connectionState.activeMethod ?: savedSession.server.activeConnection,
                            url = connectionState.activeMethod?.url ?: savedSession.server.url
                        )
                        val updatedSession = savedSession.copy(server = updatedServer)

                        val validationResult = validateSessionUseCase()
                        val isSessionValid = validationResult is NetworkResult.Success && validationResult.data
                        val isAuthError = validationResult is NetworkResult.Error && validationResult.error is AppError.Auth
                        val shouldInvalidate =
                            (validationResult is NetworkResult.Success && !validationResult.data) || isAuthError
                        val isOfflineError = connectionState.isOffline ||
                                (validationResult is NetworkResult.Error && !isAuthError)

                        if (shouldInvalidate) {
                            runCatching {
                                preferencesManager.invalidateSession()
                                preferencesManager.clearAuthData()
                            }
                        }

                        val updatedState = when {
                            isSessionValid -> {
                                _state.value.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    authSession = updatedSession,
                                    server = updatedServer,
                                    serverUrl = updatedServer.url,
                                    connectionMethods = resolvedMethods,
                                    activeConnectionMethod =
                                        connectionState.activeMethod ?: savedSession.server.activeConnection,
                                    isOfflineMode = connectionState.isOffline,
                                    isWifiConnected = connectionState.isWifiConnected,
                                    localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                    remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                    error = null
                                )
                            }
                            isOfflineError -> {
                                _state.value.copy(
                                    isLoading = false,
                                    isAuthenticated = true,
                                    authSession = updatedSession,
                                    server = updatedServer,
                                    serverUrl = updatedServer.url,
                                    connectionMethods = resolvedMethods,
                                    activeConnectionMethod =
                                        connectionState.activeMethod ?: savedSession.server.activeConnection,
                                    isOfflineMode = true,
                                    isWifiConnected = connectionState.isWifiConnected,
                                    localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                    remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                    error = (validationResult as? NetworkResult.Error)?.message
                                        ?: connectionState.lastMessage
                                )
                            }
                            else -> {
                                val failureMessage = when (validationResult) {
                                    is NetworkResult.Error -> validationResult.message
                                    is NetworkResult.Success -> "Session expired. Please log in again."
                                    else -> null
                                }
                                _state.value.copy(
                                    isLoading = false,
                                    isAuthenticated = false,
                                    authSession = null,
                                    server = updatedServer,
                                    serverUrl = updatedServer.url,
                                    connectionMethods = resolvedMethods,
                                    activeConnectionMethod =
                                        connectionState.activeMethod ?: savedSession.server.activeConnection,
                                    isOfflineMode = false,
                                    isWifiConnected = connectionState.isWifiConnected,
                                    localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                                    remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                                    error = failureMessage
                                )
                            }
                        }
                        _state.value = updatedState
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            isAuthenticated = false
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isAuthenticated = false,
                        error = result.message
                    )
                }
                is NetworkResult.Loading -> {
                    _state.value = _state.value.copy(isLoading = true)
                }
            }
            _state.value = _state.value.copy(initializationState = InitializationState.Initialized)
        }
    }

    private fun loadSavedServer() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            when (val result = getSavedServerUseCase()) {
                is NetworkResult.Success -> {
                    if (result.data != null) {
                        val connectionState = serverConnectionManager.state.value
                        val resolvedMethods = connectionState.methods.ifEmpty { result.data.connectionMethods }
                        val updatedServer = result.data.copy(
                            connectionMethods = resolvedMethods,
                            activeConnection = connectionState.activeMethod ?: result.data.activeConnection,
                            url = connectionState.activeMethod?.url ?: result.data.url
                        )
                        _state.value = _state.value.copy(
                            server = updatedServer,
                            serverUrl = updatedServer.url,
                            connectionMethods = resolvedMethods,
                            activeConnectionMethod = connectionState.activeMethod ?: result.data.activeConnection,
                            isOfflineMode = connectionState.isOffline,
                            isWifiConnected = connectionState.isWifiConnected,
                            localConnectionUrls = resolvedMethods.filter { it.isLocal }.map { it.url },
                            remoteConnectionUrls = resolvedMethods.filterNot { it.isLocal }.map { it.url },
                            isConnected = false,
                            isLoading = false
                        )
                    } else {
                        _state.value = _state.value.copy(isLoading = false)
                    }
                }
                is NetworkResult.Error -> {
                    _state.value = _state.value.copy(
                        error = result.message,
                        isLoading = false
                    )
                }
                is NetworkResult.Loading -> {
                    
                }
            }
        }
    }

    private fun cleanServerUrl(url: String): String = serverConnectionManager.normalizeUrl(url)

    private fun getDisplayName(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetConnectionState() {
        _state.value = _state.value.copy(
            isConnected = false,
            isLoading = false,
            error = null,
            mtlsError = null
        )
    }

    companion object {
        private const val LOGIN_TIMEOUT_MESSAGE = "Login timed out. Please try again."
    }
}

@Serializable
private data class ServerPushPayload(
    val serverUrl: String,
    val port: Int,
    val connectionType: String,
    val fullUrl: String,
    val mediaToken: String,
    val mTLS: Boolean,
    val password: String
)
