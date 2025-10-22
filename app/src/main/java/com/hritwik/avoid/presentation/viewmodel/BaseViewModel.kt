package com.hritwik.avoid.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import kotlinx.coroutines.flow.StateFlow

open class BaseViewModel(
    connectivityObserver: ConnectivityObserver
) : ViewModel() {
    val isConnected: StateFlow<Boolean> = connectivityObserver.isConnected
}
