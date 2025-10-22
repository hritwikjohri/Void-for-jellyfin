package com.hritwik.avoid.presentation.viewmodel.service

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.core.ServiceEventBus
import com.hritwik.avoid.core.ServiceHealth
import com.hritwik.avoid.core.ServiceManager
import com.hritwik.avoid.data.download.DownloadService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ServiceDashboardViewModel @Inject constructor(
    private val serviceManager: ServiceManager
) : ViewModel() {

    val downloads = serviceManager.downloads

    private val _events = MutableSharedFlow<ServiceEventBus.Event>()
    val events: SharedFlow<ServiceEventBus.Event> = _events.asSharedFlow()

    init {
        viewModelScope.launch {
            ServiceEventBus.events.collect { _events.emit(it) }
        }
    }

    fun isDownloadServiceRunning(): Boolean = serviceManager.get(DownloadService::class) != null

    fun downloadHealth(): ServiceHealth? = serviceManager.getHealth(DownloadService::class)

    fun pause(id: String) = serviceManager.pauseDownload(id)
    fun resume(id: String) = serviceManager.resumeDownload(id)
    fun cancel(id: String) = serviceManager.cancelDownload(id)
}

