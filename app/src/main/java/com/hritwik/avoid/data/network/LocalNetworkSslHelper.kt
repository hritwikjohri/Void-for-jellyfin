package com.hritwik.avoid.data.network

import java.net.Inet4Address
import java.net.InetAddress
import java.net.UnknownHostException
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager
import kotlin.math.max
import kotlin.math.min


object LocalNetworkSslHelper {

    private val localRanges = listOf(
        IpRange("10.0.0.0", "10.255.255.255"),
        IpRange("127.0.0.0", "127.255.255.255"),
        IpRange("169.254.0.0", "169.254.255.255"),
        IpRange("172.16.0.0", "172.31.255.255"),
        IpRange("192.168.0.0", "192.168.255.255"),
        IpRange("224.0.0.0", "255.255.255.255"),
    )

    private val defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier()

    fun createSslConfig(clientKeyManager: X509ExtendedKeyManager? = null): LocalNetworkSslConfig {
        val trustManager = LocalNetworkTrustManager(createDefaultTrustManager())
        val sslContext = SSLContext.getInstance("TLS")
        val keyManagers = clientKeyManager?.let { arrayOf<KeyManager>(it) }
        sslContext.init(keyManagers, arrayOf<TrustManager>(trustManager), null)
        return LocalNetworkSslConfig(
            sslSocketFactory = sslContext.socketFactory,
            trustManager = trustManager,
            hostnameVerifier = LocalNetworkHostnameVerifier(defaultHostnameVerifier)
        )
    }

    fun isLocalNetworkHost(host: String?): Boolean {
        if (host.isNullOrBlank()) return false
        return try {
            val address = InetAddress.getByName(host)
            if (address !is Inet4Address) return false
            val ipValue = address.toLong()
            localRanges.any { range -> range.contains(ipValue) }
        } catch (_: UnknownHostException) {
            false
        }
    }

    private fun createDefaultTrustManager(): X509ExtendedTrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as java.security.KeyStore?)
        val trustManagers = factory.trustManagers
        val extended = trustManagers.filterIsInstance<X509ExtendedTrustManager>().firstOrNull()
        if (extended != null) {
            return extended
        }
        val basic = trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
            ?: throw IllegalStateException("No X509TrustManager found")
        return object : X509ExtendedTrustManager() {
            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) = basic.checkClientTrusted(chain, authType)

            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String,
                socket: java.net.Socket
            ) = basic.checkClientTrusted(chain, authType)

            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String,
                engine: SSLEngine
            ) = basic.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) = basic.checkServerTrusted(chain, authType)

            override fun checkServerTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String,
                socket: java.net.Socket
            ) = basic.checkServerTrusted(chain, authType)

            override fun checkServerTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String,
                engine: SSLEngine
            ) = basic.checkServerTrusted(chain, authType)

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = basic.acceptedIssuers
        }
    }

    private data class IpRange(val start: Long, val end: Long) {
        constructor(start: String, end: String) : this(start.toIpLong(), end.toIpLong())

        fun contains(value: Long): Boolean {
            val lower = min(start, end)
            val upper = max(start, end)
            return value in lower..upper
        }
    }

    private fun Inet4Address.toLong(): Long {
        val bytes = address
        var result = 0L
        for (byte in bytes) {
            result = (result shl 8) or (byte.toInt() and 0xFF).toLong()
        }
        return result
    }

    private fun String.toIpLong(): Long {
        val parts = split('.')
        require(parts.size == 4) { "Invalid IPv4 address: $this" }
        var result = 0L
        for (part in parts) {
            val value = part.toInt()
            require(value in 0..255) { "Invalid IPv4 address: $this" }
            result = (result shl 8) or value.toLong()
        }
        return result
    }
}


data class LocalNetworkSslConfig(
    val sslSocketFactory: SSLSocketFactory,
    val trustManager: X509TrustManager,
    val hostnameVerifier: HostnameVerifier,
)

private class LocalNetworkHostnameVerifier(
    private val defaultVerifier: HostnameVerifier,
) : HostnameVerifier {
    override fun verify(hostname: String?, session: SSLSession?): Boolean {
        if (LocalNetworkSslHelper.isLocalNetworkHost(hostname)) {
            return true
        }
        return defaultVerifier.verify(hostname, session)
    }
}

private class LocalNetworkTrustManager(
    private val delegate: X509ExtendedTrustManager,
) : X509ExtendedTrustManager() {

    override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
        delegate.checkClientTrusted(chain, authType)
    }

    override fun checkClientTrusted(
        chain: Array<java.security.cert.X509Certificate>,
        authType: String,
        socket: java.net.Socket
    ) {
        delegate.checkClientTrusted(chain, authType, socket)
    }

    override fun checkClientTrusted(
        chain: Array<java.security.cert.X509Certificate>,
        authType: String,
        engine: javax.net.ssl.SSLEngine
    ) {
        delegate.checkClientTrusted(chain, authType, engine)
    }

    override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {
        delegate.checkServerTrusted(chain, authType)
    }

    override fun checkServerTrusted(
        chain: Array<java.security.cert.X509Certificate>,
        authType: String,
        socket: java.net.Socket
    ) {
        if (socket is SSLSocket && LocalNetworkSslHelper.isLocalNetworkHost(socket.hostAddress())) {
            return
        }
        delegate.checkServerTrusted(chain, authType, socket)
    }

    override fun checkServerTrusted(
        chain: Array<java.security.cert.X509Certificate>,
        authType: String,
        engine: javax.net.ssl.SSLEngine
    ) {
        if (LocalNetworkSslHelper.isLocalNetworkHost(engine.peerHost)) {
            return
        }
        delegate.checkServerTrusted(chain, authType, engine)
    }

    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = delegate.acceptedIssuers

    private fun SSLSocket.hostAddress(): String? {
        return inetAddress?.hostAddress
            ?: inetAddress?.hostName
            ?: session?.peerHost
    }
}
