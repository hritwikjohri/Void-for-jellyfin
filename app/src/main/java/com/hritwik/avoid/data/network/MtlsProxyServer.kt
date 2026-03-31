package com.hritwik.avoid.data.network

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.hritwik.avoid.data.local.PreferencesManager
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.Charsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class MtlsProxyServer @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val preferencesManager: PreferencesManager,
) {
    companion object {
        private const val TAG = "MtlsProxyServer"
        private const val DEFAULT_REASON = "OK"
        private val STATUS_REASONS = mapOf(
            200 to "OK",
            201 to "Created",
            202 to "Accepted",
            204 to "No Content",
            206 to "Partial Content",
            301 to "Moved Permanently",
            302 to "Found",
            304 to "Not Modified",
            307 to "Temporary Redirect",
            308 to "Permanent Redirect",
            400 to "Bad Request",
            401 to "Unauthorized",
            403 to "Forbidden",
            404 to "Not Found",
            405 to "Method Not Allowed",
            416 to "Range Not Satisfiable",
            500 to "Internal Server Error",
            502 to "Bad Gateway",
            503 to "Service Unavailable",
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val proxyRequired = AtomicBoolean(false)
    private val stateInitialized = AtomicBoolean(false)
    private val serverRunning = AtomicBoolean(false)

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var acceptJobActive = false

    @Volatile
    private var localPort: Int = -1

    init {
        scope.launch {
            combine(
                preferencesManager.isMtlsEnabled(),
                preferencesManager.getMtlsCertificateName(),
            ) { enabled, certificateName ->
                enabled && !certificateName.isNullOrBlank()
            }.collect { required ->
                proxyRequired.set(required)
                stateInitialized.set(true)
                if (!required) {
                    stopServer()
                }
            }
        }
    }

    fun proxiedUrlFor(originalUrl: String?): String? {
        if (originalUrl.isNullOrBlank()) return originalUrl
        if (!shouldProxy(originalUrl)) return originalUrl
        val port = runCatching { ensureServerStarted() }
            .onFailure { error -> Log.e(TAG, "Failed to start proxy", error) }
            .getOrElse { return originalUrl }
        val encoded = Base64.encodeToString(
            originalUrl.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        return Uri.Builder()
            .scheme("http")
            .encodedAuthority("127.0.0.1:$port")
            .appendPath("proxy")
            .appendPath(encoded)
            .build()
            .toString()
    }

    @Synchronized
    private fun ensureServerStarted(): Int {
        val existing = serverSocket
        if (existing != null && serverRunning.get()) {
            return existing.localPort
        }
        val socket = ServerSocket(0, 0, InetAddress.getByName("127.0.0.1")).apply {
            reuseAddress = true
        }
        serverSocket = socket
        localPort = socket.localPort
        serverRunning.set(true)
        if (!acceptJobActive) {
            acceptJobActive = true
            scope.launch { acceptConnections(socket) }
        }
        return localPort
    }

    @Synchronized
    private fun stopServer() {
        serverRunning.set(false)
        acceptJobActive = false
        runCatching { serverSocket?.close() }
        serverSocket = null
        localPort = -1
    }

    private suspend fun acceptConnections(server: ServerSocket) {
        while (serverRunning.get()) {
            try {
                val client = server.accept()
                scope.launch { handleClient(client) }
            } catch (error: SocketException) {
                if (serverRunning.get()) {
                    Log.w(TAG, "Proxy accept failed", error)
                }
                break
            } catch (error: IOException) {
                Log.e(TAG, "Proxy accept error", error)
                break
            }
        }
        acceptJobActive = false
    }

    private fun shouldProxy(url: String): Boolean {
        val scheme = runCatching { Uri.parse(url)?.scheme?.lowercase(Locale.ROOT) }
            .getOrNull()
        if (scheme == null || scheme == "file" || scheme == "content") {
            return false
        }
        if (!stateInitialized.get()) {
            val required = runBlocking(Dispatchers.IO) {
                val enabled = preferencesManager.isMtlsEnabled().first()
                val certificateName = preferencesManager.getMtlsCertificateName().first()
                enabled && !certificateName.isNullOrBlank()
            }
            proxyRequired.set(required)
            stateInitialized.set(true)
        }
        return proxyRequired.get()
    }

    private suspend fun handleClient(client: Socket) {
        client.use { socket ->
            socket.soTimeout = 30_000
            val input = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
            val output = BufferedOutputStream(socket.getOutputStream())
            var headersSent = false
            try {
                val request = parseRequest(input) ?: run {
                    writeError(output, 400, "Bad Request")
                    return
                }
                if (request.method != "GET" && request.method != "HEAD") {
                    writeError(output, 405, "Method Not Allowed")
                    return
                }
                val targetUrl = decodeTargetUrl(request.path)
                    ?: run {
                        writeError(output, 400, "Invalid Proxy Target")
                        return
                    }
                val requestBuilder = Request.Builder().url(targetUrl)
                request.headers.forEach { (name, value) ->
                    if (!name.equals("Host", true) && !name.equals("Connection", true)) {
                        requestBuilder.header(name, value)
                    }
                }
                if (request.method == "HEAD") {
                    requestBuilder.head()
                } else {
                    requestBuilder.get()
                }
                okHttpClient.newCall(requestBuilder.build()).execute().use { response ->
                    val rewrittenBody = if (shouldRewritePlaylist(targetUrl, response)) {
                        response.body?.string()?.let { original ->
                            rewritePlaylistBody(targetUrl, original)
                        }
                    } else {
                        null
                    }
                    val reason = response.message.ifBlank {
                        STATUS_REASONS[response.code] ?: DEFAULT_REASON
                    }
                    output.write("HTTP/1.1 ${response.code} $reason\r\n".toByteArray(StandardCharsets.US_ASCII))
                    for (name in response.headers.names()) {
                        if (!name.equals("Connection", true) && !(rewrittenBody != null && name.equals("Content-Length", true))) {
                            val values = response.headers.values(name)
                            values.forEach { value ->
                                output.write("$name: $value\r\n".toByteArray(StandardCharsets.US_ASCII))
                            }
                        }
                    }
                    rewrittenBody?.let { bodyBytes ->
                        output.write("Content-Length: ${bodyBytes.size}\r\n".toByteArray(StandardCharsets.US_ASCII))
                    }
                    output.write("Connection: close\r\n\r\n".toByteArray(StandardCharsets.US_ASCII))
                    headersSent = true
                    if (request.method != "HEAD") {
                        val bodyStream = response.body
                        if (rewrittenBody != null) {
                            output.write(rewrittenBody)
                        } else {
                            bodyStream?.byteStream()?.use { body ->
                                body.copyTo(output)
                            }
                        }
                    }
                    output.flush()
                }
            } catch (error: Exception) {
                if (!headersSent) {
                    writeError(output, 502, "Bad Gateway")
                }
                Log.e(TAG, "Proxy client handling failed", error)
            } finally {
                runCatching { input.close() }
                runCatching { output.flush() }
            }
        }
    }

    private fun parseRequest(reader: BufferedReader): HttpRequest? {
        val requestLine = reader.readLine() ?: return null
        if (requestLine.isBlank()) return null
        val parts = requestLine.split(' ')
        if (parts.size < 3) return null
        val headers = mutableMapOf<String, String>()
        while (true) {
            val line = reader.readLine() ?: break
            if (line.isEmpty()) break
            val index = line.indexOf(':')
            if (index != -1) {
                val name = line.substring(0, index).trim()
                val value = line.substring(index + 1).trim()
                headers[name] = value
            }
        }
        return HttpRequest(parts[0], parts[1], parts[2], headers)
    }

    private fun decodeTargetUrl(path: String): String? {
        val localUri = Uri.parse("http://localhost$path")
        val segments = localUri.pathSegments
        if (segments.isEmpty() || segments[0] != "proxy") return null
        val encoded = segments.getOrNull(1) ?: return null
        return try {
            val bytes = Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
            String(bytes, Charsets.UTF_8)
        } catch (error: IllegalArgumentException) {
            Log.e(TAG, "Failed to decode proxy target", error)
            null
        }
    }

    private fun shouldRewritePlaylist(targetUrl: String, response: okhttp3.Response): Boolean {
        val contentType = response.header("Content-Type")?.lowercase(Locale.ROOT)
        if (contentType != null) {
            if (contentType.contains("application/vnd.apple.mpegurl") ||
                contentType.contains("application/x-mpegurl") ||
                contentType.contains("audio/mpegurl") ||
                contentType.contains("application/mpegurl")
            ) {
                return true
            }
        }
        val uri = runCatching { URI(targetUrl) }.getOrNull() ?: return false
        val path = uri.path?.lowercase(Locale.ROOT) ?: return false
        return path.endsWith(".m3u8")
    }

    private fun rewritePlaylistBody(targetUrl: String, originalBody: String): ByteArray {
        val baseUri = runCatching { URI(targetUrl) }.getOrNull()
        if (baseUri == null) {
            return originalBody.toByteArray(Charsets.UTF_8)
        }
        val rewritten = buildString {
            originalBody.lineSequence().forEachIndexed { index, rawLine ->
                if (index > 0) append('\n')
                val hasCarriageReturn = rawLine.endsWith('\r')
                val line = rawLine.trimEnd('\r')
                if (line.isBlank() || line.startsWith("#")) {
                    append(line)
                } else {
                    val resolved = runCatching { baseUri.resolve(line) }.getOrNull()
                    val proxied = resolved?.toString()?.let { proxiedUrlFor(it) }
                    append(proxied ?: line)
                }
                if (hasCarriageReturn) {
                    append('\r')
                }
            }
        }
        return rewritten.toByteArray(Charsets.UTF_8)
    }

    private fun writeError(output: BufferedOutputStream, code: Int, message: String) {
        val body = "$message\n"
        val reason = STATUS_REASONS[code] ?: DEFAULT_REASON
        val header = buildString {
            append("HTTP/1.1 $code $reason\r\n")
            append("Content-Type: text/plain; charset=utf-8\r\n")
            append("Content-Length: ")
            append(body.toByteArray(Charsets.UTF_8).size)
            append("\r\n")
            append("Connection: close\r\n\r\n")
        }
        output.write(header.toByteArray(Charsets.UTF_8))
        output.write(body.toByteArray(Charsets.UTF_8))
        output.flush()
    }

    private data class HttpRequest(
        val method: String,
        val path: String,
        val version: String,
        val headers: Map<String, String>,
    )
}
