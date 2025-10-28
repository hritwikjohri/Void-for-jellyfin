package com.hritwik.avoid.data.network

import android.util.Log
import com.hritwik.avoid.data.local.PreferencesManager
import java.io.ByteArrayInputStream
import java.net.Socket
import java.security.KeyStore
import java.security.Principal
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLEngine
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509KeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Singleton
class MtlsCertificateProvider @Inject constructor(
    private val preferencesManager: PreferencesManager,
) {

    companion object {
        private const val TAG = "MtlsCertificateProvider"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val initialLoad = AtomicBoolean(false)

    @Volatile
    private var activeKeyManager: X509ExtendedKeyManager? = null

    private val delegatingKeyManager = DelegatingMtlsKeyManager { activeKeyManager }

    init {
        scope.launch {
            combine(
                preferencesManager.isMtlsEnabled(),
                preferencesManager.getMtlsCertificateName(),
                preferencesManager.getMtlsCertificatePassword(),
            ) { enabled, name, password ->
                Triple(enabled, name, password)
            }.collectLatest { (enabled, name, password) ->
                applyState(enabled, name, password)
            }
        }
    }

    fun keyManager(): X509ExtendedKeyManager {
        ensureInitialized()
        return delegatingKeyManager
    }

    private suspend fun loadKeyManager(password: String): X509ExtendedKeyManager? {
        val certificateBytes = preferencesManager.getMtlsCertificateBytes() ?: return null
        return withContext(Dispatchers.IO) {
            runCatching {
                val passwordChars = password.toCharArray()
                try {
                    val keyStore = KeyStore.getInstance("PKCS12")
                    ByteArrayInputStream(certificateBytes).use { input ->
                        keyStore.load(input, passwordChars)
                    }
                    val keyManagerFactory = KeyManagerFactory.getInstance(
                        KeyManagerFactory.getDefaultAlgorithm()
                    )
                    keyManagerFactory.init(keyStore, passwordChars)
                    keyManagerFactory.keyManagers
                        .filterIsInstance<X509KeyManager>()
                        .firstOrNull()
                        ?.toExtended()
                } finally {
                    passwordChars.fill('\u0000')
                }
            }.onFailure { error ->
                Log.e(TAG, "Failed to load mTLS certificate", error)
            }.getOrNull()
        }
    }

    private suspend fun applyState(
        enabled: Boolean,
        certificateName: String?,
        password: String?,
    ) {
        activeKeyManager = if (!enabled || certificateName.isNullOrBlank()) {
            null
        } else {
            loadKeyManager(password.orEmpty())
        }
    }

    private fun ensureInitialized() {
        if (initialLoad.compareAndSet(false, true)) {
            runBlocking {
                applyState(
                    preferencesManager.isMtlsEnabled().first(),
                    preferencesManager.getMtlsCertificateName().first(),
                    preferencesManager.getMtlsCertificatePassword().first(),
                )
            }
        }
    }
}

private class DelegatingMtlsKeyManager(
    private val delegateProvider: () -> X509ExtendedKeyManager?,
) : X509ExtendedKeyManager() {

    override fun getClientAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? {
        return delegateProvider()?.getClientAliases(keyType, issuers)
    }

    override fun chooseClientAlias(
        keyType: Array<String>?,
        issuers: Array<Principal>?,
        socket: Socket?,
    ): String? {
        return delegateProvider()?.chooseClientAlias(keyType, issuers, socket)
    }

    override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
        return delegateProvider()?.getCertificateChain(alias)
    }

    override fun getPrivateKey(alias: String?): PrivateKey? {
        return delegateProvider()?.getPrivateKey(alias)
    }

    override fun getServerAliases(keyType: String?, issuers: Array<Principal>?): Array<String>? {
        return delegateProvider()?.getServerAliases(keyType, issuers)
    }

    override fun chooseServerAlias(
        keyType: String?,
        issuers: Array<Principal>?,
        socket: Socket?,
    ): String? {
        return delegateProvider()?.chooseServerAlias(keyType, issuers, socket)
    }

    override fun chooseEngineClientAlias(
        keyType: Array<String>?,
        issuers: Array<Principal>?,
        engine: SSLEngine?,
    ): String? {
        return delegateProvider()?.chooseEngineClientAlias(keyType, issuers, engine)
    }

    override fun chooseEngineServerAlias(
        keyType: String?,
        issuers: Array<Principal>?,
        engine: SSLEngine?,
    ): String? {
        return delegateProvider()?.chooseEngineServerAlias(keyType, issuers, engine)
    }
}

private fun X509KeyManager.toExtended(): X509ExtendedKeyManager {
    return if (this is X509ExtendedKeyManager) {
        this
    } else {
        object : X509ExtendedKeyManager() {
            override fun getClientAliases(
                keyType: String?,
                issuers: Array<Principal>?,
            ): Array<String>? = this@toExtended.getClientAliases(keyType, issuers)

            override fun chooseClientAlias(
                keyType: Array<String>?,
                issuers: Array<Principal>?,
                socket: Socket?,
            ): String? {
                val types = keyType ?: return null
                return this@toExtended.chooseClientAlias(types, issuers, socket)
            }

            override fun getCertificateChain(alias: String?): Array<X509Certificate>? {
                return this@toExtended.getCertificateChain(alias)
            }

            override fun getPrivateKey(alias: String?): PrivateKey? {
                return this@toExtended.getPrivateKey(alias)
            }

            override fun getServerAliases(
                keyType: String?,
                issuers: Array<Principal>?,
            ): Array<String>? = this@toExtended.getServerAliases(keyType, issuers)

            override fun chooseServerAlias(
                keyType: String?,
                issuers: Array<Principal>?,
                socket: Socket?,
            ): String? {
                return this@toExtended.chooseServerAlias(keyType, issuers, socket)
            }

            override fun chooseEngineClientAlias(
                keyType: Array<String>?,
                issuers: Array<Principal>?,
                engine: SSLEngine?,
            ): String? {
                val types = keyType ?: return null
                return this@toExtended.chooseClientAlias(types, issuers, null)
            }

            override fun chooseEngineServerAlias(
                keyType: String?,
                issuers: Array<Principal>?,
                engine: SSLEngine?,
            ): String? {
                return this@toExtended.chooseServerAlias(keyType, issuers, null)
            }
        }
    }
}
