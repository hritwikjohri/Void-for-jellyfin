package com.hritwik.avoid.presentation.ui.state

data class QuickConnectState(
    val code: String? = null,
    val secret: String? = null,
    val isPolling: Boolean = false,
    val error: String? = null
)