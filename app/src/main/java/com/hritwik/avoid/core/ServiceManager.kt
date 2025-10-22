package com.hritwik.avoid.core

import android.app.Service
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.data.download.DownloadService
import com.hritwik.avoid.data.download.DownloadServiceManager
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.utils.helpers.normalizeUuid
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass

@OptIn(UnstableApi::class)
@Singleton
class ServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadDao: DownloadDao,
) {

    private val registered = mutableSetOf<KClass<out Service>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            ServiceEventBus.events.collect { event ->
                Log.d("ServiceManager", "event: $event")
            }
        }
    }

    fun <T : Service> register(service: KClass<T>) {
        registered.add(service)
    }

    fun <T : Service> stop(service: KClass<T>) {
        context.stopService(Intent(context, service.java))
    }

    fun getHealth(service: KClass<out Service>): ServiceHealth? {
        return get(service) as? ServiceHealth
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Service> get(service: KClass<T>): T? {
        return when (service) {
            DownloadService::class -> DownloadService.getInstance() as? T
            else -> null
        }
    }

    fun shutdown() {
        registered.toList().forEach { stop(it) }
        registered.clear()
    }

    fun startDownload(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        request: DownloadRequest,
        priority: Int = 0,
        mediaSourceId: String? = null,
    ) {
        DownloadService.startDownload(
            context = context,
            mediaItem = mediaItem,
            serverUrl = serverUrl,
            accessToken = accessToken,
            request = request,
            priority = priority,
            mediaSourceId = mediaSourceId
        )
    }

    fun pauseDownload(id: String) {
        DownloadService.pauseDownload(context, id)
    }

    fun resumeDownload(id: String) {
        DownloadService.resumeDownload(context, id)
    }

    fun cancelDownload(id: String) {
        DownloadService.cancelDownload(context, id)
    }

    fun pauseAllDownloads() {
        DownloadServiceManager.withService(context) { it.pauseAllDownloads() }
    }

    suspend fun getDownloadedFilePath(id: String): String? {
        return DownloadService.getDownloadedFilePath(context, id)
            ?: withContext(Dispatchers.IO) {
                downloadDao.getDownloadByMediaSourceId(id)?.filePath
                    ?: downloadDao.getDownloadByMediaId(normalizeUuid(id))?.filePath
            }
    }

    fun cleanupOrphanedDownloads() {
        DownloadService.cleanupOrphanedDownloads(context)
    }

    val downloads get() = DownloadService.downloads
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ServiceManagerEntryPoint {
    fun serviceManager(): ServiceManager
}

