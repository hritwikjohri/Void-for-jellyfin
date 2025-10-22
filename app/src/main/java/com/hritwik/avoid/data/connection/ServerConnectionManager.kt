package com.hritwik.avoid.data.connection

import android.os.SystemClock
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod
import com.hritwik.avoid.domain.model.auth.ServerConnectionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import com.hritwik.avoid.utils.constants.ApiConstants.ENDPOINT_SERVER_PING
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import java.io.IOException
import java.net.URI
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerConnectionManager @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val connectivityObserver: ConnectivityObserver,
    okHttpClient: OkHttpClient
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val messageState = MutableStateFlow<String?>(null)
    private val _state = MutableStateFlow(ServerConnectionState())
    val state: StateFlow<ServerConnectionState> = _state.asStateFlow()
    private val _events = MutableSharedFlow<ServerConnectionEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ServerConnectionEvent> = _events.asSharedFlow()
    @Volatile
    private var lastEvaluationTimedOut: Boolean = false
    private val pingClient = okHttpClient.newBuilder()
        .connectTimeout(PER_PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(PER_PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(PER_PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(PER_PING_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val evaluationMutex = Mutex()
    private var offlineRecheckJob: Job? = null

    init {
        scope.launch {
            combine(
                preferencesManager.getServerConnections(),
                preferencesManager.getServerUrl(),
                preferencesManager.getOfflineMode(),
                connectivityObserver.isOnWifi,
                messageState
            ) { methods, activeUrl, offline, onWifi, message ->
                val sanitized = sanitizeMethods(methods)
                val active = activeUrl?.let { url ->
                    sanitized.firstOrNull { it.url.equals(url, ignoreCase = true) }
                }
                ServerConnectionState(
                    methods = sanitized,
                    activeMethod = active,
                    isOffline = offline,
                    isWifiConnected = onWifi,
                    lastMessage = message
                )
            }.collect { _state.value = it }
        }

        scope.launch {
            connectivityObserver.isConnected.collect { connected ->
                if (connected) {
                    refreshActiveConnection(messageOnSwitch = "Reconnected to preferred connection")
                } else {
                    setOffline(true, "No network connection")
                }
            }
        }

        scope.launch {
            var lastWifi = connectivityObserver.isOnWifi.value
            connectivityObserver.isOnWifi.collect { onWifi ->
                if (onWifi != lastWifi) {
                    val message = if (onWifi) {
                        "Reconnected to local server"
                    } else {
                        "Switched to internet connection"
                    }
                    refreshActiveConnection(messageOnSwitch = message)
                    lastWifi = onWifi
                }
            }
        }
    }

    suspend fun setLocalAddresses(urls: List<String>) {
        val normalized = urls.mapNotNull { address ->
            address.trim().takeIf { it.isNotEmpty() }?.let { normalizeUrl(it) }
        }
        evaluationMutex.withLock {
            val existing = sanitizeMethods(preferencesManager.getServerConnections().first())
            val remotes = existing.filterNot { it.isLocal }
            val locals = normalized.distinctBy { it.lowercase(Locale.US) }
                .map { address -> ServerConnectionMethod(address, ServerConnectionType.LOCAL) }
            val updated = locals + remotes
            preferencesManager.saveServerConnections(updated)
        }
        refreshActiveConnection(messageOnSwitch = null)
    }

    suspend fun setRemoteAddresses(urls: List<String>) {
        val normalized = urls.mapNotNull { address ->
            address.trim().takeIf { it.isNotEmpty() }?.let { normalizeUrl(it) }
        }
        evaluationMutex.withLock {
            val existing = sanitizeMethods(preferencesManager.getServerConnections().first())
            val locals = existing.filter { it.isLocal }
            val remotes = normalized.distinctBy { it.lowercase(Locale.US) }
                .map { address -> ServerConnectionMethod(address, ServerConnectionType.REMOTE) }
            val updated = locals + remotes
            preferencesManager.saveServerConnections(updated)
        }
        refreshActiveConnection(messageOnSwitch = null)
    }

    suspend fun clearConnectionState() {
        evaluationMutex.withLock {
            preferencesManager.saveServerConnections(emptyList())
            preferencesManager.setOfflineMode(false)
            messageState.value = null
            _state.value = ServerConnectionState()
        }
    }

    suspend fun refreshActiveConnection(
        messageOnSwitch: String? = null,
        excludeUrl: String? = null
    ): ServerConnectionMethod? = evaluationMutex.withLock {
        if (!connectivityObserver.isConnected.value) {
            lastEvaluationTimedOut = false
            setOffline(true, "No network connection")
            return@withLock null
        }

        val methods = ensureStoredMethods()
        if (methods.isEmpty()) {
            lastEvaluationTimedOut = false
            setOffline(true, "No connection methods configured")
            return@withLock null
        }

        val activeUrl = preferencesManager.getServerUrl().first()
        val activeMethod = activeUrl?.let { url ->
            methods.firstOrNull { it.url.equals(url, ignoreCase = true) }
        }

        val prioritized = buildList {
            val locals = methods.filter { it.isLocal }
            val remotes = methods.filterNot { it.isLocal }
            val ordered = when {
                connectivityObserver.isOnWifi.value -> locals + remotes
                remotes.isNotEmpty() -> remotes + locals
                else -> locals
            }
            ordered.forEach { method ->
                if (excludeUrl == null || !method.url.equals(excludeUrl, ignoreCase = true)) {
                    add(method)
                }
            }
        }

        return@withLock try {
            withTimeout(NETWORK_CHECK_TIMEOUT_MS) {
                val wifiConnected = connectivityObserver.isOnWifi.value
                var timeoutEncountered = false

                val localCandidate = if (wifiConnected) {
                    evaluateMethods(prioritized.filter { it.isLocal }) {
                        timeoutEncountered = timeoutEncountered || it
                    }
                } else {
                    null
                }

                val remoteCandidate = evaluateMethods(prioritized.filterNot { it.isLocal }) {
                    timeoutEncountered = timeoutEncountered || it
                }

                val chosen = localCandidate ?: remoteCandidate

                if (chosen != null) {
                    lastEvaluationTimedOut = false
                    val notify = if (chosen.url != activeMethod?.url) messageOnSwitch else null
                    activateMethod(chosen, notify)
                } else {
                    lastEvaluationTimedOut = timeoutEncountered
                    if (timeoutEncountered) {
                        setOffline(true, NETWORK_TIMEOUT_MESSAGE)
                    } else {
                        setOffline(true, "All connection methods failed")
                    }
                    null
                }
            }
        } catch (timeout: TimeoutCancellationException) {
            lastEvaluationTimedOut = true
            setOffline(true, NETWORK_TIMEOUT_MESSAGE)
            null
        }
    }


    suspend fun markRequestSuccess(url: String?) {
        if (url.isNullOrBlank()) return
        val normalized = normalizeUrl(url)
        evaluationMutex.withLock {
            ensureMethodExists(normalized)
            preferencesManager.saveServerUrlOnly(normalized)
            preferencesManager.setOfflineMode(false)
            messageState.value = null
        }
        cancelOfflineRecheck()
    }

    suspend fun markRequestFailure(url: String?, message: String? = null) {
        val normalized = url?.takeIf { it.isNotBlank() }?.let { normalizeUrl(it) }
        val switchMessage = message ?: "Switched to backup server connection"
        val result = refreshActiveConnection(
            messageOnSwitch = switchMessage,
            excludeUrl = normalized
        )
        if (result == null) {
            if (!lastEvaluationTimedOut) {
                setOffline(true, message ?: "All connection methods failed")
            }
        }
    }

    suspend fun ensureActiveConnection() {
        refreshActiveConnection()
    }

    private suspend fun activateMethod(
        method: ServerConnectionMethod,
        message: String?
    ): ServerConnectionMethod {
        val previousMethod = _state.value.activeMethod
        preferencesManager.saveServerUrlOnly(method.url)
        preferencesManager.setOfflineMode(false)
        val notifyMessage = when {
            message != null && previousMethod?.url != method.url -> "$message (${method.url})"
            previousMethod != null && previousMethod.url != method.url -> {
                if (method.isLocal) {
                    "Connected to local server: ${method.url}"
                } else {
                    "Using internet connection: ${method.url}"
                }
            }
            previousMethod == null -> "Connected to: ${method.url}"
            else -> null
        }
        messageState.value = notifyMessage
        if (!notifyMessage.isNullOrBlank()) {
            _events.emit(ServerConnectionEvent.MethodSwitched(method, notifyMessage))
        }
        cancelOfflineRecheck()
        return method
    }

    private suspend fun setOffline(enabled: Boolean, message: String?) {
        preferencesManager.setOfflineMode(enabled)
        if (enabled) {
            val finalMessage = message ?: "All connection methods failed"
            if (_state.value.isOffline.not() || _state.value.lastMessage != finalMessage) {
                messageState.value = finalMessage
                _events.emit(ServerConnectionEvent.Offline(finalMessage))
            } else {
                messageState.value = finalMessage
            }
            scheduleOfflineRecheck()
        } else if (_state.value.isOffline) {
            messageState.value = null
            cancelOfflineRecheck()
        }
    }

    private suspend fun ensureMethodExists(normalizedUrl: String): List<ServerConnectionMethod> {
        val existing = sanitizeMethods(preferencesManager.getServerConnections().first())
        if (existing.any { it.url.equals(normalizedUrl, ignoreCase = true) }) {
            preferencesManager.saveServerConnections(existing)
            return existing
        }
        val type = ServerConnectionType.fromUrl(normalizedUrl)
        val updated = existing + ServerConnectionMethod(normalizedUrl, type)
        preferencesManager.saveServerConnections(updated)
        return updated
    }

    private suspend fun ensureStoredMethods(): List<ServerConnectionMethod> {
        val existing = sanitizeMethods(preferencesManager.getServerConnections().first())
        if (existing.isNotEmpty()) {
            preferencesManager.saveServerConnections(existing)
            return existing
        }
        val storedUrl = preferencesManager.getServerUrl().first()
        if (!storedUrl.isNullOrBlank()) {
            val normalized = normalizeUrl(storedUrl)
            val base = ServerConnectionMethod(normalized, ServerConnectionType.fromUrl(normalized))
            preferencesManager.saveServerConnections(listOf(base))
            return listOf(base)
        }
        return emptyList()
    }

    private fun sanitizeMethods(methods: List<ServerConnectionMethod>): List<ServerConnectionMethod> {
        if (methods.isEmpty()) return emptyList()
        val seen = mutableSetOf<String>()
        return methods.map { method ->
            val normalizedUrl = normalizeUrl(method.url)
            val type = ServerConnectionType.fromUrl(normalizedUrl)
            ServerConnectionMethod(normalizedUrl, type)
        }.filter { seen.add(it.url.lowercase()) }
    }

    private suspend fun evaluateMethods(
        methods: List<ServerConnectionMethod>,
        timeoutListener: (Boolean) -> Unit
    ): ServerConnectionMethod? {
        if (methods.isEmpty()) {
            timeoutListener(false)
            return null
        }
        var timeoutOccurred = false
        for (method in methods) {
            when (checkReachable(method.url)) {
                ReachabilityResult.Reachable -> {
                    timeoutListener(timeoutOccurred)
                    return method
                }
                ReachabilityResult.Timeout -> {
                    timeoutOccurred = true
                }
                ReachabilityResult.Unreachable -> Unit
            }
        }
        timeoutListener(timeoutOccurred)
        return null
    }

    private fun scheduleOfflineRecheck() {
        if (offlineRecheckJob?.isActive == true) return
        offlineRecheckJob = scope.launch {
            try {
                while (isActive) {
                    delay(OFFLINE_RECHECK_INTERVAL_MS)
                    if (!connectivityObserver.isConnected.value) {
                        continue
                    }
                    val result = refreshActiveConnection(messageOnSwitch = "Automatically restored connection")
                    if (result != null) {
                        break
                    }
                }
            } finally {
                offlineRecheckJob = null
            }
        }
    }

    private fun cancelOfflineRecheck() {
        offlineRecheckJob?.cancel()
        offlineRecheckJob = null
    }

    private suspend fun checkReachable(url: String): ReachabilityResult = withContext(Dispatchers.IO) {
        val candidates = buildReachabilityTargets(url)
        if (candidates.isEmpty()) {
            return@withContext ReachabilityResult.Unreachable
        }

        val start = SystemClock.elapsedRealtime()
        var attempts = 0
        for (candidate in candidates) {
            attempts++
            val base = if (candidate.endsWith("/")) candidate else "$candidate/"
            val request = Request.Builder()
                .url(base + PING_PATH)
                .get()
                .build()
            val reachable = try {
                withTimeout(TimeUnit.SECONDS.toMillis(PER_PING_TIMEOUT_SECONDS)) {
                    pingClient.newCall(request).execute().use { response ->
                        response.isSuccessful
                    }
                }
            } catch (_: TimeoutCancellationException) {
                return@withContext ReachabilityResult.Timeout
            } catch (_: IOException) {
                continue
            }
            if (reachable) {
                return@withContext ReachabilityResult.Reachable
            }
        }

        val elapsed = SystemClock.elapsedRealtime() - start
        if (elapsed >= TimeUnit.SECONDS.toMillis(PER_PING_TIMEOUT_SECONDS) * attempts) {
            ReachabilityResult.Timeout
        } else {
            ReachabilityResult.Unreachable
        }
    }

    private fun buildReachabilityTargets(url: String): List<String> {
        val normalized = normalizeUrl(url)
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return listOf(normalized)
        if (uri.port != -1) {
            return listOf(normalized)
        }

        val userInfo = uri.userInfo?.takeIf { it.isNotBlank() }
        val host = uri.host ?: return listOf(normalized)
        val path = uri.path
        val query = uri.query
        val fragment = uri.fragment
        val baseScheme = (uri.scheme ?: "http").lowercase(Locale.US)

        val targets = mutableListOf(normalized)
        for (port in FALLBACK_PORTS) {
            val schemes = if (port == 443) {
                listOf(baseScheme, "https")
            } else {
                listOf(baseScheme, "http")
            }.distinct()
            for (scheme in schemes) {
                val candidate = URI(
                    scheme,
                    userInfo,
                    host,
                    port,
                    path,
                    query,
                    fragment
                ).toString()
                targets.add(normalizeUrl(candidate))
            }
        }
        return targets.distinct()
    }

    fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "http://$clean"
        }
        while (clean.endsWith("/")) {
            clean = clean.dropLast(1)
        }
        return clean
    }

    companion object {
        private const val PER_PING_TIMEOUT_SECONDS = 1L
        private const val NETWORK_CHECK_TIMEOUT_MS = 5_000L
        private const val NETWORK_TIMEOUT_MESSAGE = "Network check timed out after 5 seconds."
        private const val OFFLINE_RECHECK_INTERVAL_MS = 10_000L
        private const val PING_PATH = ENDPOINT_SERVER_PING
        private val FALLBACK_PORTS = listOf(8096, 80, 443)
    }

    private enum class ReachabilityResult {
        Reachable,
        Timeout,
        Unreachable
    }
}
