package com.hritwik.avoid.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.data.connection.ServerConnectionManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

open class BaseViewModel(
    protected val serverConnectionManager: ServerConnectionManager
) : ViewModel() {
    val isConnected: StateFlow<Boolean> = serverConnectionManager.state
        .map { state -> !state.isOffline && state.activeMethod != null }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            !serverConnectionManager.state.value.isOffline &&
                serverConnectionManager.state.value.activeMethod != null
        )
}
