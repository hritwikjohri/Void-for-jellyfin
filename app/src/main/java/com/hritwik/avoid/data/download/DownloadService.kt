package com.hritwik.avoid.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.MainActivity
import com.hritwik.avoid.data.download.toDownloadInfo
import com.hritwik.avoid.domain.model.download.DownloadCodec
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.entities.DownloadEntity
import com.hritwik.avoid.data.local.database.entities.MediaItemEntity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.FileVisitResult
import java.nio.file.attribute.BasicFileAttributes
import java.io.IOException
import com.hritwik.avoid.utils.helpers.getDeviceName
import com.hritwik.avoid.utils.helpers.normalizeUuid
import kotlin.collections.ArrayDeque
import kotlin.math.max
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.hritwik.avoid.R
import com.hritwik.avoid.data.network.LocalNetworkSslHelper
import com.hritwik.avoid.data.network.MtlsCertificateProvider
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.data.remote.dto.library.BaseItemDto

private const val TAG = "DownloadService"
private const val SERVICE_NOTIFICATION_ID = 0x444f574e
private const val MAX_CONCURRENT_DOWNLOADS = 2
private const val DOWNLOAD_THROTTLE_CHUNK_BYTES = 512 * 1024L
private const val DOWNLOAD_THROTTLE_DELAY_MS = 50L

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadServiceEntryPoint {
    fun downloadDao(): DownloadDao
    fun mediaItemDao(): MediaItemDao
    fun preferencesManager(): PreferencesManager
    fun mtlsCertificateProvider(): MtlsCertificateProvider
}

@UnstableApi
class DownloadService : LifecycleService() {

    inner class DownloadBinder : Binder() {
        fun getService(): DownloadService = this@DownloadService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    private val binder = DownloadBinder()

    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadServiceEntryPoint::class.java
        )
    }

    private val client by lazy {
        val sslConfig = LocalNetworkSslHelper.createSslConfig(
            entryPoint.mtlsCertificateProvider().keyManager()
        )
        OkHttpClient.Builder()
            .sslSocketFactory(sslConfig.sslSocketFactory, sslConfig.trustManager)
            .hostnameVerifier(sslConfig.hostnameVerifier)
            .build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val downloadDir by lazy {
        File(applicationContext.filesDir, "downloads").apply { mkdirs() }
    }
    private val json = Json { ignoreUnknownKeys = true }
    private val deviceId by lazy { getDeviceName(applicationContext) }
    private val maxConcurrent = MAX_CONCURRENT_DOWNLOADS
    private val queue = ArrayDeque<DownloadTask>()
    private val active = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val lock = Any()
    private val downloadDao: DownloadDao by lazy { entryPoint.downloadDao() }
    private val mediaItemDao: MediaItemDao by lazy { entryPoint.mediaItemDao() }
    private val preferencesManager: PreferencesManager by lazy { entryPoint.preferencesManager() }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private var isForegroundService = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceNotificationRunnable = Runnable { updateServiceNotificationOnMain() }

    data class DownloadTask(val id: String, val info: DownloadInfo)

    data class DownloadInfo(
        val mediaItem: MediaItem,
        val requestUri: String,
        val file: File,
        val progress: Float,
        val status: DownloadStatus,
        val serverUrl: String,
        val accessToken: String,
        val priority: Int,
        val addedAt: Long,
        val quality: DownloadQuality,
        val request: DownloadRequest,
        val mediaSourceId: String? = null,
        val downloadedBytes: Long = 0L,
        val totalBytes: Long? = null
    )

    enum class DownloadStatus { QUEUED, DOWNLOADING, PAUSED, COMPLETED, FAILED }

    companion object {
        private val _downloads = MutableStateFlow<Map<String, DownloadInfo>>(emptyMap())
        val downloads: StateFlow<Map<String, DownloadInfo>> = _downloads.asStateFlow()

        private val _downloadProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
        val downloadProgress: StateFlow<Map<String, Float>> = _downloadProgress.asStateFlow()

        private val _downloadStatuses = MutableStateFlow<Map<String, DownloadStatus>>(emptyMap())
        val downloadStatuses: StateFlow<Map<String, DownloadStatus>> = _downloadStatuses.asStateFlow()

        @Volatile
        private var instance: DownloadService? = null

        private data class PendingDownload(
            val mediaItem: MediaItem,
            val serverUrl: String,
            val accessToken: String,
            val request: DownloadRequest,
            val priority: Int,
            val mediaSourceId: String?
        )

        private val pendingDownloads = mutableListOf<PendingDownload>()
        private val pendingServiceActions = mutableListOf<(DownloadService) -> Unit>()

        private fun enqueueServiceAction(
            context: Context,
            startIfNeeded: Boolean,
            action: (DownloadService) -> Unit
        ) {
            val instance = getInstance()
            if (instance != null) {
                action(instance)
                return
            }
            if (!startIfNeeded) return

            synchronized(pendingServiceActions) {
                pendingServiceActions.add(action)
            }
            start(context)
        }

        fun start(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, DownloadService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
        }

        fun getInstance(): DownloadService? = instance

        fun startDownload(
            context: Context,
            mediaItem: MediaItem,
            serverUrl: String,
            accessToken: String,
            request: DownloadRequest,
            priority: Int = 0,
            mediaSourceId: String? = null
        ) {
            start(context)
            val instance = getInstance()
            if (instance != null) {
                instance.queueDownload(mediaItem, serverUrl, accessToken, request, priority, mediaSourceId)
            } else {
                synchronized(pendingDownloads) {
                    pendingDownloads.add(
                        PendingDownload(mediaItem, serverUrl, accessToken, request, priority, mediaSourceId)
                    )
                }
            }
        }

        fun pauseDownload(context: Context, id: String) {
            getInstance()?.pause(id)
        }

        fun resumeDownload(context: Context, id: String) {
            enqueueServiceAction(context, startIfNeeded = true) { it.resume(id) }
        }

        fun resumeAllDownloads(context: Context) {
            enqueueServiceAction(context, startIfNeeded = true) { it.resumeAllDownloads() }
        }

        fun cancelDownload(context: Context, id: String) {
            val instance = getInstance()
            if (instance != null) {
                instance.cancel(id)
            } else {
                val appContext = context.applicationContext
                val dao = EntryPointAccessors.fromApplication(
                    appContext,
                    DownloadServiceEntryPoint::class.java
                ).downloadDao()
                val removalKeys = runBlocking(Dispatchers.IO) {
                    val entity = dao.getDownloadByMediaSourceId(id)
                        ?: dao.getDownloadByMediaSourceId(normalizeUuid(id))
                        ?: dao.getDownloadByMediaId(id)
                        ?: dao.getDownloadByMediaId(normalizeUuid(id))
                    val candidates = buildCandidateIds(id, entity)
                    candidates.forEach { candidate ->
                        deleteFolderImmediate(File(appContext.filesDir, "downloads/$candidate"))
                    }
                    if (entity != null) {
                        entity.filePath?.let { deleteFolderImmediate(File(it).parentFile) }
                        dao.deleteDownload(entity)
                    }
                    candidates
                }
                clearDownloadState(removalKeys)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                removalKeys.forEach { key -> notificationManager?.cancel(key.hashCode()) }
            }
        }

        private fun buildCandidateIds(
            originalId: String,
            entity: DownloadEntity?
        ): Set<String> {
            return buildSet {
                addAll(candidateForms(originalId))
                entity?.mediaSourceId?.let { addAll(candidateForms(it)) }
                entity?.mediaId?.let { addAll(candidateForms(it)) }
            }.filterNot { it.isBlank() }.toSet()
        }

        private fun candidateForms(id: String): Set<String> {
            val normalized = normalizeUuid(id)
            val compact = normalized.replace("-", "")
            return setOf(id, normalized, compact)
        }

        private fun clearDownloadState(ids: Set<String>) {
            if (ids.isEmpty()) return
            _downloads.update { map -> map - ids }
            _downloadProgress.update { map -> map - ids }
            _downloadStatuses.update { map -> map - ids }
            synchronized(pendingDownloads) {
                if (pendingDownloads.isNotEmpty()) {
                    pendingDownloads.removeAll { pending ->
                        val pendingIds = buildSet {
                            addAll(candidateForms(pending.mediaItem.id))
                            pending.mediaSourceId?.let { addAll(candidateForms(it)) }
                        }
                        pendingIds.any { it in ids }
                    }
                }
            }
        }

        fun getDownloadedFilePath(context: Context, id: String): String? {
            val instance = getInstance()
            if (instance != null) return instance.getDownloadedFilePathInternal(id)
            val appContext = context.applicationContext
            val dao = EntryPointAccessors.fromApplication(
                appContext,
                DownloadServiceEntryPoint::class.java
            ).downloadDao()
            return runBlocking {
                dao.getDownloadByMediaSourceId(id)?.filePath
                    ?: dao.getDownloadByMediaId(normalizeUuid(id))?.filePath
            }
        }

        fun cleanupOrphanedDownloads(context: Context) {
            getInstance()?.cleanupOrphanedDownloadsInternal()
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        scope.launch {
            restoreDownloadsFromDatabase()
            synchronized(pendingDownloads) {
                pendingDownloads.forEach {
                    queueDownload(it.mediaItem, it.serverUrl, it.accessToken, it.request, it.priority, it.mediaSourceId)
                }
                pendingDownloads.clear()
            }
            val actions = synchronized(pendingServiceActions) {
                pendingServiceActions.toList().also { pendingServiceActions.clear() }
            }
            actions.forEach { it(this@DownloadService) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        scope.cancel()
        mainHandler.removeCallbacks(serviceNotificationRunnable)
        if (isForegroundService) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            notificationManager.cancel(SERVICE_NOTIFICATION_ID)
            isForegroundService = false
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            DownloadNotificationService.CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.download_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private suspend fun restoreDownloadsFromDatabase() {
        val stored = downloadDao.getAllDownloads().first()
        if (stored.isEmpty()) {
            updateServiceNotification()
            return
        }
        stored.forEach { entity ->
            val id = entity.mediaSourceId ?: entity.mediaId
            var info = entity.toDownloadInfo(json)
            if (info.status == DownloadStatus.COMPLETED) {
                registerDownloadInfo(id, info, notify = false)
                return@forEach
            }
            val tmpFile = info.file.parentFile?.resolve("${info.file.nameWithoutExtension}.tmp")
            val existingBytes = when {
                tmpFile?.exists() == true -> tmpFile.length()
                info.file.exists() -> {
                    if (tmpFile != null) {
                        tmpFile.parentFile?.mkdirs()
                        info.file.renameTo(tmpFile)
                        tmpFile.length()
                    } else {
                        info.file.length()
                    }
                }
                else -> 0L
            }
            val resumedBytes = max(info.downloadedBytes, existingBytes)
            if (resumedBytes > info.downloadedBytes) {
                info = info.copy(downloadedBytes = resumedBytes)
            }
            val statusForResume = when (info.status) {
                DownloadStatus.DOWNLOADING -> DownloadStatus.QUEUED
                else -> info.status
            }
            if (statusForResume != info.status) {
                info = info.copy(status = statusForResume)
            }
            var updatedEntity = entity
            if (resumedBytes > entity.downloadedBytes) {
                updatedEntity = updatedEntity.copy(downloadedBytes = resumedBytes)
            }
            if (statusForResume.name != entity.status) {
                updatedEntity = updatedEntity.copy(status = statusForResume.name)
            }
            if (info.progress != entity.progress) {
                updatedEntity = updatedEntity.copy(progress = info.progress)
            }
            if (updatedEntity != entity) {
                downloadDao.updateDownload(updatedEntity)
            }
            registerDownloadInfo(id, info, notify = statusForResume != DownloadStatus.PAUSED)
            if (statusForResume == DownloadStatus.QUEUED) {
                synchronized(lock) { queue.addLast(DownloadTask(id, info)) }
            }
        }
        updateServiceNotification()
        schedule()
    }

    private fun registerDownloadInfo(id: String, info: DownloadInfo, notify: Boolean = true) {
        _downloads.update { it + (id to info) }
        _downloadProgress.update { it + (id to info.progress) }
        _downloadStatuses.update { it + (id to info.status) }
        if (notify) {
            showNotification(id, info)
        }
    }

    private fun showNotification(id: String, info: DownloadInfo) {
        val notification = DownloadNotificationService.buildNotification(this, info)
        notificationManager.notify(id.hashCode(), notification)
    }

    private fun updateServiceNotification() {
        mainHandler.removeCallbacks(serviceNotificationRunnable)
        mainHandler.post(serviceNotificationRunnable)
    }

    private fun updateServiceNotificationOnMain() {
        val statuses = _downloadStatuses.value.values
        val hasActive = statuses.any {
            it == DownloadStatus.DOWNLOADING ||
                it == DownloadStatus.QUEUED ||
                it == DownloadStatus.PAUSED
        }
        if (!hasActive) {
            if (isForegroundService) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                notificationManager.cancel(SERVICE_NOTIFICATION_ID)
                isForegroundService = false
            }
            val shouldStop = synchronized(lock) { active.isEmpty() && queue.isEmpty() }
            if (shouldStop) {
                stopSelf()
            }
            return
        }

        val notification = buildServiceNotification(statuses)
        if (!isForegroundService) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    SERVICE_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(SERVICE_NOTIFICATION_ID, notification)
            }
            isForegroundService = true
        } else {
            notificationManager.notify(SERVICE_NOTIFICATION_ID, notification)
        }
    }

    private fun buildServiceNotification(statuses: Collection<DownloadStatus>): Notification {
        val downloadingCount = statuses.count { it == DownloadStatus.DOWNLOADING }
        val queuedCount = statuses.count { it == DownloadStatus.QUEUED }
        val pausedCount = statuses.count { it == DownloadStatus.PAUSED }

        val parts = mutableListOf<String>()
        if (downloadingCount > 0) {
            parts += resources.getQuantityString(
                R.plurals.download_summary_downloading,
                downloadingCount,
                downloadingCount
            )
        }
        if (queuedCount > 0) {
            parts += resources.getQuantityString(
                R.plurals.download_summary_queued,
                queuedCount,
                queuedCount
            )
        }
        if (pausedCount > 0) {
            parts += resources.getQuantityString(
                R.plurals.download_summary_paused,
                pausedCount,
                pausedCount
            )
        }

        val contentText = if (parts.isEmpty()) {
            getString(R.string.download_channel_description)
        } else {
            parts.joinToString(separator = " • ")
        }

        return NotificationCompat.Builder(this, DownloadNotificationService.CHANNEL_ID)
            .setContentTitle(getString(R.string.download_channel_name))
            .setContentText(contentText)
            .setSmallIcon(R.drawable.void_icon)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(createDownloadsPendingIntent())
            .build()
    }

    private fun createDownloadsPendingIntent(): PendingIntent {
        val downloadsIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            SERVICE_NOTIFICATION_ID,
            downloadsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun queueDownload(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        request: DownloadRequest,
        priority: Int,
        mediaSourceId: String?
    ) {
        val itemId = normalizeUuid(mediaItem.id)
        val sourceId = mediaSourceId
            ?: mediaItem.getPrimaryMediaSource()?.id
            ?: itemId
        if (_downloads.value.containsKey(sourceId)) {
            Log.d(TAG, "Download already queued for source $sourceId")
            return
        }

        val extension = mediaItem.getPrimaryMediaSource()?.container ?: "mkv"
        val itemDir = File(downloadDir, sourceId).apply { mkdirs() }
        val file = File(itemDir, "$sourceId.$extension")
        val posterFile = File(itemDir, "poster.jpg")
        val logoFile = File(itemDir, "logo.png")
        var offlineItem = mediaItem.copy(
            id = itemId,
            primaryImageTag = posterFile.absolutePath,
            logoImageTag = mediaItem.logoImageTag?.let { logoFile.absolutePath },
            backdropImageTags = listOf(posterFile.absolutePath)
        )

        val requestUrl = buildString {
            append(serverUrl.removeSuffix("/"))
            append("/Videos/")
            append(itemId)
            append("/stream.")
            append(extension)
            append("?static=")
            append(request.static)
            append("&mediaSourceId=")
            append(sourceId)
            request.maxWidth?.let {
                append("&MaxWidth=")
                append(it)
            }
            request.maxHeight?.let {
                append("&MaxHeight=")
                append(it)
            }
            request.maxBitrate?.let {
                append("&MaxBitrate=")
                append(it)
            }
            request.videoBitrate?.let {
                append("&VideoBitrate=")
                append(it)
            }
            request.audioBitrate?.let {
                append("&AudioBitrate=")
                append(it)
            }
            append("&EnableAutoStreamCopy=")
            append(request.enableAutoStreamCopy)
            append("&AllowVideoStreamCopy=")
            append(request.allowVideoStreamCopy)
            append("&AllowAudioStreamCopy=")
            append(request.allowAudioStreamCopy)
            append("&VideoCodec=")
            append(request.videoCodec.serverValue)
            if (!request.static) {
                request.videoCodec.profile?.let { profile ->
                    append("&Profile=")
                    append(profile)
                }
            }
            if (request.audioCodec.isNotBlank()) {
                append("&AudioCodec=")
                append(request.audioCodec)
            }
            if (request.copySubtitles) {
                append("&CopySubtitles=true")
                append("&SubtitleMethod=Embed")
            }
            if (request.copyFontData) {
                append("&EnableSubtitlesInManifest=true")
                append("&CopyTimestamps=true")
            }
        }

        val info = DownloadInfo(
            offlineItem,
            requestUrl,
            file,
            0f,
            DownloadStatus.QUEUED,
            serverUrl,
            accessToken,
            priority,
            System.currentTimeMillis(),
            request.quality,
            request,
            sourceId,
            downloadedBytes = 0L,
            totalBytes = null
        )
        Log.d(TAG, "Queue download $sourceId -> ${info.requestUri}")
        _downloads.update { it + (sourceId to info) }
        _downloadProgress.update { it + (sourceId to 0f) }
        _downloadStatuses.update { it + (sourceId to DownloadStatus.QUEUED) }
        showNotification(sourceId, info)

        val source = mediaItem.getPrimaryMediaSource()
        scope.launch {
            val entity = DownloadEntity(
                mediaId = itemId,
                requestUri = requestUrl,
                title = mediaItem.name,
                type = mediaItem.type,
                serverUrl = serverUrl,
                accessToken = accessToken,
                progress = 0f,
                status = DownloadStatus.QUEUED.name,
                downloadedBytes = 0L,
                filePath = file.absolutePath,
                mediaSourceId = sourceId,
                defaultVideoStream = source?.defaultVideoStream,
                audioStreams = source?.audioStreams ?: emptyList(),
                subtitleStreams = source?.subtitleStreams ?: emptyList(),
                priority = priority,
                addedAt = System.currentTimeMillis(),
                quality = request.quality.name
            )

            val userId = preferencesManager.getUserId().first() ?: ""
            val runtimeFromItem = mediaItem.runTimeTicks?.takeIf { it > 0 }
            val resolvedRuntimeTicks = runtimeFromItem
                ?: fetchItemDurationTicks(itemId, serverUrl, accessToken, userId)
            if (resolvedRuntimeTicks != null && resolvedRuntimeTicks > 0 &&
                offlineItem.runTimeTicks != resolvedRuntimeTicks
            ) {
                offlineItem = offlineItem.copy(runTimeTicks = resolvedRuntimeTicks)
                updateDownloadMediaItem(sourceId, offlineItem)
            }
            if (userId.isNotEmpty()) {
                val existing = mediaItemDao.getMediaItem(itemId, userId)
                if (existing == null) {
                    val placeholder = MediaItemEntity(
                        id = itemId,
                        name = mediaItem.name,
                        title = mediaItem.title,
                        type = mediaItem.type,
                        overview = mediaItem.overview,
                        year = mediaItem.year,
                        communityRating = mediaItem.communityRating,
                        runTimeTicks = resolvedRuntimeTicks ?: mediaItem.runTimeTicks,
                        primaryImageTag = mediaItem.primaryImageTag,
                        thumbImageTag = mediaItem.thumbImageTag,
                        backdropImageTags = mediaItem.backdropImageTags,
                        genres = mediaItem.genres,
                        isFolder = mediaItem.isFolder,
                        childCount = mediaItem.childCount,
                        libraryId = null,
                        userId = userId,
                        isFavorite = mediaItem.userData?.isFavorite ?: false,
                        playbackPositionTicks = mediaItem.userData?.playbackPositionTicks ?: 0L,
                        playCount = mediaItem.userData?.playCount ?: 0,
                        played = mediaItem.userData?.played ?: false,
                        lastPlayedDate = mediaItem.userData?.lastPlayedDate,
                        isWatchlist = mediaItem.userData?.isWatchlist ?: false,
                        pendingFavorite = mediaItem.userData?.pendingFavorite ?: false,
                        pendingPlayed = mediaItem.userData?.pendingPlayed ?: false,
                        pendingWatchlist = mediaItem.userData?.pendingWatchlist ?: false,
                        taglines = mediaItem.taglines
                    )
                    mediaItemDao.insertMediaItem(placeholder)
                } else if (
                    resolvedRuntimeTicks != null && resolvedRuntimeTicks > 0 &&
                    existing.runTimeTicks != resolvedRuntimeTicks
                ) {
                    mediaItemDao.updateMediaItem(existing.copy(runTimeTicks = resolvedRuntimeTicks))
                }
            }

            downloadDao.insertDownload(entity)
            val jsonFile = File(itemDir, "$sourceId.json")
            runCatching { jsonFile.writeText(json.encodeToString(offlineItem)) }
            downloadImage(serverUrl, itemId, mediaItem.primaryImageTag, "Primary", posterFile)
            downloadImage(serverUrl, itemId, mediaItem.logoImageTag, "Logo", logoFile)
        }

        synchronized(lock) {
            queue.addLast(DownloadTask(sourceId, info))
        }
        schedule()
        updateServiceNotification()
    }

    private fun schedule() {
        val tasksToStart = mutableListOf<DownloadTask>()
        synchronized(lock) {
            var availableSlots = (maxConcurrent - active.size).coerceAtLeast(0)
            while (availableSlots > 0 && queue.isNotEmpty()) {
                tasksToStart += queue.removeFirst()
                availableSlots--
            }
        }
        tasksToStart.forEach { launchDownload(it) }
    }

    private suspend fun fetchItemDurationTicks(
        itemId: String,
        serverUrl: String,
        accessToken: String,
        userId: String
    ): Long? {
        if (userId.isEmpty()) return null
        val url = buildString {
            append(serverUrl.removeSuffix("/"))
            append("/Users/")
            append(userId)
            append("/Items/")
            append(itemId)
            append("?Fields=RunTimeTicks")
        }
        val request = Request.Builder()
            .url(url)
            .addHeader(
                "X-Emby-Authorization",
                JellyfinApiService.createAuthHeader(deviceId, token = accessToken)
            )
            .build()
        return runCatching {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to fetch duration metadata for $itemId: ${response.code}")
                    return@use null
                }
                val body = response.body?.string() ?: return@use null
                json.decodeFromString<BaseItemDto>(body).runTimeTicks
            }
        }.onFailure { error ->
            Log.e(TAG, "Metadata request failed for $itemId", error)
        }.getOrNull()
    }

    private fun updateDownloadMediaItem(id: String, updatedItem: MediaItem) {
        _downloads.update { map ->
            val existing = map[id] ?: return@update map
            map + (id to existing.copy(mediaItem = updatedItem))
        }
    }

    private fun launchDownload(task: DownloadTask) {
        val id = task.id
        val info = task.info
        val job = scope.launch {
            var downloadedBytes = info.downloadedBytes
            var totalBytes: Long? = info.totalBytes
            val tmp = info.file.parentFile?.resolve("${info.file.nameWithoutExtension}.tmp")
            try {
                Log.d(TAG, "Start download $id")
                if (tmp != null && !tmp.exists() && info.file.exists()) {
                    tmp.parentFile?.mkdirs()
                    info.file.renameTo(tmp)
                }
                if (tmp != null && tmp.exists() && downloadedBytes < tmp.length()) {
                    downloadedBytes = tmp.length()
                }
                val initialProgress = if (totalBytes != null && totalBytes!! > 0 && downloadedBytes > 0) {
                    (downloadedBytes * 100f / totalBytes!!).coerceAtMost(99.9f)
                } else {
                    info.progress
                }
                updateProgress(
                    id,
                    initialProgress,
                    DownloadStatus.DOWNLOADING,
                    downloadedBytes = downloadedBytes.takeIf { it > 0L },
                    totalBytes = totalBytes
                )
                val requestBuilder = Request.Builder().url(info.requestUri)
                if (downloadedBytes > 0L) {
                    requestBuilder.header("Range", "bytes=$downloadedBytes-")
                }
                client.newCall(requestBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code} for $id")
                    }
                    val body = response.body ?: throw IOException("empty body")
                    if (downloadedBytes > 0L && response.code != 206) {
                        Log.w(TAG, "Server ignored range for $id, restarting download")
                        downloadedBytes = 0L
                        tmp?.delete()
                    }
                    val contentRange = response.header("Content-Range")
                    totalBytes = when {
                        contentRange != null -> parseContentRange(contentRange)?.takeIf { it > 0L }
                        body.contentLength() > 0L -> body.contentLength() + downloadedBytes
                        else -> totalBytes
                    }
                    val workingFile = tmp ?: File(info.file.parentFile, "${info.file.nameWithoutExtension}.tmp")
                    workingFile.parentFile?.mkdirs()
                    FileOutputStream(workingFile, downloadedBytes > 0L).use { out ->
                        val input = body.byteStream()
                        val buffer = ByteArray(64 * 1024)
                        var lastPercent = initialProgress
                        var lastBytesUpdate = SystemClock.elapsedRealtime()
                        var throttleBytes = downloadedBytes
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            downloadedBytes += read
                            if (downloadedBytes - throttleBytes >= DOWNLOAD_THROTTLE_CHUNK_BYTES) {
                                throttleBytes = downloadedBytes
                                delay(DOWNLOAD_THROTTLE_DELAY_MS)
                            }
                            val totalBytesValue = totalBytes
                            if (totalBytesValue != null && totalBytesValue > 0) {
                                val percent = (downloadedBytes * 100f / totalBytesValue).coerceAtMost(100f)
                                if (percent - lastPercent >= 1f) {
                                    lastPercent = percent
                                    lastBytesUpdate = SystemClock.elapsedRealtime()
                                    updateProgress(
                                        id,
                                        percent,
                                        DownloadStatus.DOWNLOADING,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    )
                                } else {
                                    val now = SystemClock.elapsedRealtime()
                                    if (now - lastBytesUpdate >= 1000L) {
                                        lastBytesUpdate = now
                                        updateProgress(
                                            id,
                                            lastPercent,
                                            DownloadStatus.DOWNLOADING,
                                            downloadedBytes = downloadedBytes,
                                            totalBytes = totalBytes
                                        )
                                    }
                                }
                            } else {
                                val now = SystemClock.elapsedRealtime()
                                if (now - lastBytesUpdate >= 500L) {
                                    lastBytesUpdate = now
                                    updateProgress(
                                        id,
                                        lastPercent,
                                        DownloadStatus.DOWNLOADING,
                                        downloadedBytes = downloadedBytes
                                    )
                                }
                            }
                        }
                    }
                    workingFile.renameTo(info.file)
                }
                Log.d(TAG, "Download completed $id")
                updateProgress(
                    id,
                    100f,
                    DownloadStatus.COMPLETED,
                    path = info.file.absolutePath,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes
                )
            } catch (e: CancellationException) {
                Log.d(TAG, "Download paused $id")
                val progress = _downloadProgress.value[id] ?: info.progress
                updateProgress(
                    id,
                    progress,
                    DownloadStatus.PAUSED,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes
                )
                return@launch
            } catch (e: Exception) {
                Log.e(TAG, "Download failed $id", e)
                updateProgress(
                    id,
                    _downloadProgress.value[id] ?: info.progress,
                    DownloadStatus.FAILED,
                    downloadedBytes = downloadedBytes,
                    totalBytes = totalBytes
                )
            } finally {
                synchronized(lock) {
                    active.remove(id)
                }
                schedule()
            }
        }
        synchronized(lock) {
            active[id] = job
        }
    }

    private fun resolveId(id: String): String? {
        return if (_downloads.value.containsKey(id)) {
            id
        } else {
            _downloads.value.entries.firstOrNull { it.value.mediaItem.id == id }?.key
        }
    }

    private fun pause(id: String) {
        val key = resolveId(id) ?: return
        val job = synchronized(lock) {
            active.remove(key)
        }
        job?.cancel()
        val progress = _downloadProgress.value[key] ?: 0f
        updateProgress(key, progress, DownloadStatus.PAUSED)
        schedule()
    }

    internal fun resume(id: String) {
        val key = resolveId(id) ?: return
        val info = _downloads.value[key] ?: return
        if (info.status == DownloadStatus.COMPLETED) return
        val progress = _downloadProgress.value[key] ?: 0f
        updateProgress(key, progress, DownloadStatus.QUEUED)
        synchronized(lock) {
            queue.addLast(DownloadTask(key, info))
        }
        schedule()
    }

    internal fun resumeAllDownloads() {
        val paused = _downloadStatuses.value.filterValues { it == DownloadStatus.PAUSED }.keys
        paused.forEach { resume(it) }
    }

    private fun cancel(id: String) {
        val key = resolveId(id) ?: return
        val job = synchronized(lock) {
            active.remove(key)
        }
        job?.cancel()
        synchronized(lock) {
            queue.removeAll { it.id == key }
        }
        deleteFolderImmediate(_downloads.value[key]?.file?.parentFile)
        deleteFolderImmediate(File(downloadDir, key))
        _downloads.update { it - key }
        _downloadStatuses.update { it - key }
        _downloadProgress.update { it - key }
        scope.launch {
            val entity = downloadDao.getDownloadByMediaSourceId(key)
                ?: downloadDao.getDownloadByMediaId(normalizeUuid(key))
            if (entity != null) {
                downloadDao.deleteDownload(entity)
            }
        }
        notificationManager.cancel(key.hashCode())
        updateServiceNotification()
    }


    private fun updateProgress(
        id: String,
        percent: Float,
        status: DownloadStatus,
        path: String? = null,
        downloadedBytes: Long? = null,
        totalBytes: Long? = null
    ) {
        _downloadProgress.update { it + (id to percent) }
        _downloadStatuses.update { it + (id to status) }
        _downloads.update { map ->
            val existing = map[id] ?: return@update map
            map + (
                id to existing.copy(
                    progress = percent,
                    status = status,
                    downloadedBytes = downloadedBytes ?: existing.downloadedBytes,
                    totalBytes = totalBytes ?: existing.totalBytes
                )
            )
        }
        scope.launch {
            val entity = downloadDao.getDownloadByMediaSourceId(id)
                ?: downloadDao.getDownloadByMediaId(normalizeUuid(id))
            if (entity != null) {
                downloadDao.updateDownload(
                    entity.copy(
                        progress = percent,
                        status = status.name,
                        filePath = path ?: entity.filePath,
                        downloadedBytes = downloadedBytes ?: entity.downloadedBytes
                    )
                )
            }
        }
        _downloads.value[id]?.let { showNotification(id, it) }
        updateServiceNotification()
    }

    private fun downloadImage(
        serverUrl: String,
        itemId: String,
        tag: String?,
        type: String,
        dest: File
    ) {
        if (tag == null) return
        val url = buildString {
            append(serverUrl.removeSuffix("/"))
            append("/Items/")
            append(itemId)
            append("/Images/")
            append(type)
            append("?tag=")
            append(tag)
        }
        runCatching {
            client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                val body = resp.body ?: return@use
                dest.outputStream().use { out ->
                    body.byteStream().copyTo(out)
                }
            }
        }.onFailure { e -> Log.e(TAG, "Image download failed $itemId", e) }
    }

    private fun parseContentRange(header: String): Long? {
        val slashIndex = header.lastIndexOf('/')
        if (slashIndex == -1 || slashIndex == header.length - 1) return null
        return header.substring(slashIndex + 1).toLongOrNull()
    }

    private fun getDownloadedFilePathInternal(id: String): String? {
        val key = resolveId(id) ?: id
        _downloads.value[key]?.file?.let { if (it.exists()) return it.absolutePath }
        return runBlocking {
            downloadDao.getDownloadByMediaSourceId(key)?.filePath
                ?: downloadDao.getDownloadByMediaId(normalizeUuid(key))?.filePath
        }
    }

    private fun cleanupOrphanedDownloadsInternal() {
        downloadDir.walkTopDown().filter { it.extension == "tmp" }.forEach { it.delete() }
    }

    internal fun pauseAllDownloads() {
        val queuedToPause = synchronized(lock) {
            val queued = queue.toList()
            queue.clear()
            queued
        }
        queuedToPause.forEach { task ->
            val progress = _downloadProgress.value[task.id] ?: task.info.progress
            updateProgress(task.id, progress, DownloadStatus.PAUSED)
        }
        val running = synchronized(lock) { active.keys.toList() }
        running.forEach { pause(it) }
    }
}

private fun deleteFolderImmediate(dir: File?) {
    if (dir == null || !dir.exists()) return
    Files.walkFileTree(dir.toPath(), object : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            Files.deleteIfExists(file)
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            Files.deleteIfExists(dir)
            return FileVisitResult.CONTINUE
        }
    })
}

