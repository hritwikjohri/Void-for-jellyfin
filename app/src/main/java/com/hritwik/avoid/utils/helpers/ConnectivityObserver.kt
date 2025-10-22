package com.hritwik.avoid.utils.helpers

import kotlinx.coroutines.flow.StateFlow

interface ConnectivityObserver {
    val isConnected: StateFlow<Boolean>
    val isOnWifi: StateFlow<Boolean>
}
