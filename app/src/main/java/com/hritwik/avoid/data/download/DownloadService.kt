package com.hritwik.avoid.data.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.media3.common.util.UnstableApi
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.hritwik.avoid.R
import com.hritwik.avoid.data.network.LocalNetworkSslHelper
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.data.remote.dto.library.BaseItemDto

private const val TAG = "DownloadService"

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadServiceEntryPoint {
    fun downloadDao(): DownloadDao
    fun mediaItemDao(): MediaItemDao
    fun preferencesManager(): PreferencesManager
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

    private val client by lazy {
        val sslConfig = LocalNetworkSslHelper.sslConfig
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
    private val maxConcurrent = 2
    private val queue = ArrayDeque<DownloadTask>()
    private val active = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val entryPoint by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            DownloadServiceEntryPoint::class.java
        )
    }
    private val downloadDao: DownloadDao by lazy { entryPoint.downloadDao() }
    private val mediaItemDao: MediaItemDao by lazy { entryPoint.mediaItemDao() }
    private val preferencesManager: PreferencesManager by lazy { entryPoint.preferencesManager() }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

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
        val mediaSourceId: String? = null
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

        fun start(context: Context) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, DownloadService::class.java)
            appContext.startService(intent)
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
            getInstance()?.resume(id)
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
                runBlocking(Dispatchers.IO) {
                    val entity = dao.getDownloadByMediaSourceId(id)
                        ?: dao.getDownloadByMediaId(normalizeUuid(id))
                    deleteFolderImmediate(File(appContext.filesDir, "downloads/$id"))
                    if (entity != null) {
                        entity.filePath?.let { deleteFolderImmediate(File(it).parentFile) }
                        dao.deleteDownload(entity)
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
        synchronized(pendingDownloads) {
            pendingDownloads.forEach {
                queueDownload(it.mediaItem, it.serverUrl, it.accessToken, it.request, it.priority, it.mediaSourceId)
            }
            pendingDownloads.clear()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        scope.cancel()
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

    private fun showNotification(id: String, info: DownloadInfo) {
        val notification = DownloadNotificationService.buildNotification(this, info)
        notificationManager.notify(id.hashCode(), notification)
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
            sourceId
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

        queue.addLast(DownloadTask(sourceId, info))
        schedule()
    }

    private fun schedule() {
        while (active.size < maxConcurrent && queue.isNotEmpty()) {
            val task = queue.removeFirst()
            launchDownload(task)
        }
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
            try {
                Log.d(TAG, "Start download $id")
                updateProgress(id, 0f, DownloadStatus.DOWNLOADING)
                val tmp = File(info.file.parentFile, "${info.file.nameWithoutExtension}.tmp")
                client.newCall(Request.Builder().url(info.requestUri).build()).execute().use { response ->
                    val body = response.body ?: throw java.io.IOException("empty body")
                    FileOutputStream(tmp).use { out ->
                        val input = body.byteStream()
                        val buffer = ByteArray(64 * 1024)
                        var downloaded = 0L
                        val total = body.contentLength()
                        var lastPercent = 0f
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            downloaded += read
                            if (total > 0) {
                                val percent = downloaded * 100f / total
                                if (percent - lastPercent >= 1f) {
                                    lastPercent = percent
                                    updateProgress(id, percent, DownloadStatus.DOWNLOADING)
                                }
                            }
                        }
                    }
                }
                tmp.renameTo(info.file)
                Log.d(TAG, "Download completed $id")
                updateProgress(id, 100f, DownloadStatus.COMPLETED, info.file.absolutePath)
            } catch (e: Exception) {
                Log.e(TAG, "Download failed $id", e)
                updateProgress(id, _downloadProgress.value[id] ?: 0f, DownloadStatus.FAILED)
            } finally {
                active.remove(id)
                schedule()
            }
        }
        active[id] = job
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
        active[key]?.cancel()
        active.remove(key)
        val progress = _downloadProgress.value[key] ?: 0f
        updateProgress(key, progress, DownloadStatus.PAUSED)
    }

    private fun resume(id: String) {
        val key = resolveId(id) ?: return
        val info = _downloads.value[key] ?: return
        if (info.status == DownloadStatus.COMPLETED) return
        val progress = _downloadProgress.value[key] ?: 0f
        updateProgress(key, progress, DownloadStatus.QUEUED)
        queue.addLast(DownloadTask(key, info))
        schedule()
    }

    private fun cancel(id: String) {
        val key = resolveId(id) ?: return
        active[key]?.cancel()
        active.remove(key)
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
    }


    private fun updateProgress(id: String, percent: Float, status: DownloadStatus, path: String? = null) {
        _downloadProgress.update { it + (id to percent) }
        _downloadStatuses.update { it + (id to status) }
        _downloads.update { map ->
            val existing = map[id] ?: return@update map
            map + (id to existing.copy(progress = percent, status = status))
        }
        scope.launch {
            val entity = downloadDao.getDownloadByMediaSourceId(id)
                ?: downloadDao.getDownloadByMediaId(normalizeUuid(id))
            if (entity != null) {
                downloadDao.updateDownload(
                    entity.copy(
                        progress = percent,
                        status = status.name,
                        filePath = path ?: entity.filePath
                    )
                )
            }
        }
        _downloads.value[id]?.let { showNotification(id, it) }
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
        active.keys.toList().forEach { pause(it) }
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

