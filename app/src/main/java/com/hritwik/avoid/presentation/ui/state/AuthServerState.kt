package com.hritwik.avoid.presentation.ui.state

import android.net.Uri
import com.hritwik.avoid.domain.model.auth.AuthSession
import com.hritwik.avoid.domain.model.auth.Server
import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod

data class AuthServerState(
    val initializationState: InitializationState = InitializationState.Loading,
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val authSession: AuthSession? = null,
    val isConnected: Boolean = false,
    val server: Server? = null,
    val serverUrl: String? = null,
    val connectionMethods: List<ServerConnectionMethod> = emptyList(),
    val activeConnectionMethod: ServerConnectionMethod? = null,
    val isOfflineMode: Boolean = false,
    val isWifiConnected: Boolean = false,
    val localConnectionUrls: List<String> = emptyList(),
    val remoteConnectionUrls: List<String> = emptyList(),
    val isMtlsEnabled: Boolean = false,
    val mtlsCertificateName: String? = null,
    val mtlsCertificatePassword: String = "",
    val mtlsError: String? = null,
    val isMtlsImporting: Boolean = false,
    val error: String? = null,
    val quickConnectState: QuickConnectState = QuickConnectState(),
    val serverPushAddress: String = "",
    val isServerPushInProgress: Boolean = false,
    val serverPushFeedback: String? = null,
    val serverPushSuccess: Boolean? = null,
    val serverPushFileName: String? = null,
    val serverPushFileUri: Uri? = null,
    val serverPushPassword: String = ""
)
