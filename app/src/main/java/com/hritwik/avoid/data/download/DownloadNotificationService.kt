package com.hritwik.avoid.data.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.os.IBinder
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import coil.Coil
import coil.request.ImageRequest
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.FileHelper
import com.hritwik.avoid.utils.helpers.ImageHelper
import com.hritwik.avoid.MainActivity
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlin.math.roundToInt
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*

@OptIn(UnstableApi::class)
class DownloadNotificationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID) ?: return START_NOT_STICKY
        createChannel()
        val info = DownloadService.downloads.value[id]

        if (info != null) {
            val basicNotification = buildBasicNotification(this, info)
            startForeground(id.hashCode(), basicNotification)
            serviceScope.launch {
                try {
                    val enhancedNotification = buildNotificationWithImage(this@DownloadNotificationService, info)
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(id.hashCode(), enhancedNotification)
                } catch (e: Exception) {
                    e.printStackTrace()
                    val fallbackNotification = buildBasicNotification(this@DownloadNotificationService, info)
                    val notificationManager = getSystemService(NotificationManager::class.java)
                    notificationManager.notify(id.hashCode(), fallbackNotification)
                }
            }
        } else {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.download_channel_name))
                .setContentText(getString(R.string.download_channel_description))
                .setSmallIcon(R.drawable.void_icon)
                .build()
            startForeground(id.hashCode(), notification)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.download_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.download_channel_description)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "download_channel"
        const val EXTRA_ID = "extra_download_id"
        const val ACTION_PAUSE_RESUME = "com.hritwik.avoid.action.PAUSE_RESUME"
        const val ACTION_CANCEL = "com.hritwik.avoid.action.CANCEL"

        fun start(context: Context, id: String) {
            val intent = Intent(context, DownloadNotificationService::class.java).apply {
                putExtra(EXTRA_ID, id)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        private fun buildBasicNotification(
            context: Context,
            info: DownloadService.DownloadInfo
        ): Notification {
            val mediaItem = info.mediaItem
            val displayTitle = if (mediaItem.type.equals("Episode", true)) {
                val season = mediaItem.parentIndexNumber ?: 0
                val episode = mediaItem.indexNumber ?: 0
                val code = String.format("S%02dE%02d", season, episode)
                "${mediaItem.seriesName} $code"
            } else {
                mediaItem.name
            }

            val progressInt = info.progress.roundToInt()
            val progressFormatted = String.format("%.1f", info.progress)
            val status = info.status

            val contentText = when (status) {
                DownloadService.DownloadStatus.COMPLETED -> context.getString(R.string.download_complete)
                DownloadService.DownloadStatus.PAUSED -> "${context.getString(R.string.paused)} • $progressFormatted%"
                DownloadService.DownloadStatus.DOWNLOADING -> "${context.getString(R.string.downloading)} • $progressFormatted%"
                DownloadService.DownloadStatus.QUEUED -> context.getString(R.string.queued)
                DownloadService.DownloadStatus.FAILED -> context.getString(R.string.download_failed)
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(displayTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.void_icon)
                .setOngoing(
                    status != DownloadService.DownloadStatus.COMPLETED &&
                        status != DownloadService.DownloadStatus.FAILED
            )

            if (status != DownloadService.DownloadStatus.COMPLETED &&
                status != DownloadService.DownloadStatus.FAILED
            ) {
                
                builder.setProgress(100, progressInt, status == DownloadService.DownloadStatus.QUEUED)

                val pauseResumeText = when (status) {
                    DownloadService.DownloadStatus.DOWNLOADING -> context.getString(R.string.pause)
                    DownloadService.DownloadStatus.PAUSED -> context.getString(R.string.resume)
                    DownloadService.DownloadStatus.QUEUED -> context.getString(R.string.pause)
                    else -> context.getString(R.string.pause)
                }

                val pauseResumeIcon = when (status) {
                    DownloadService.DownloadStatus.DOWNLOADING -> android.R.drawable.ic_media_pause
                    DownloadService.DownloadStatus.PAUSED -> android.R.drawable.ic_media_play
                    DownloadService.DownloadStatus.QUEUED -> android.R.drawable.ic_media_pause
                    else -> android.R.drawable.ic_media_pause
                }

                builder.addAction(
                    pauseResumeIcon,
                    pauseResumeText,
                    actionIntent(context, ACTION_PAUSE_RESUME, mediaItem.id)
                )

                builder.addAction(
                    android.R.drawable.ic_delete,
                    context.getString(android.R.string.cancel),
                    actionIntent(context, ACTION_CANCEL, mediaItem.id)
                )
            } else {
                builder.setProgress(0, 0, false)
                builder.setAutoCancel(true)

                val file = info.file
                val uri = FileHelper.getContentUri(context, file)
                val mime = FileHelper.getMimeType(file) ?: "*/*"
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    "${mediaItem.id}_open".hashCode(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val downloadsIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val downloadsPendingIntent = PendingIntent.getActivity(
                    context,
                    "${mediaItem.id}_downloads".hashCode(),
                    downloadsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                builder.setContentIntent(downloadsPendingIntent)

                builder.addAction(
                    android.R.drawable.ic_menu_view,
                    context.getString(R.string.open),
                    openPendingIntent
                )
            }

            return builder.build()
        }

        private suspend fun buildNotificationWithImage(
            context: Context,
            info: DownloadService.DownloadInfo
        ): Notification = withContext(Dispatchers.IO) {
            val entryPoint = EntryPointAccessors.fromApplication(
                context.applicationContext,
                DownloadNotificationEntryPoint::class.java
            )
            val imageLoader = Coil.imageLoader(context)
            val imageHelper = entryPoint.imageHelper()

            val mediaItem = info.mediaItem
            val displayTitle = if (mediaItem.type.equals("Episode", true)) {
                val season = mediaItem.parentIndexNumber ?: 0
                val episode = mediaItem.indexNumber ?: 0
                val code = String.format("S%02dE%02d", season, episode)
                "${mediaItem.seriesName} $code"
            } else {
                mediaItem.name
            }

            val progressInt = info.progress.roundToInt()
            val progressFormatted = String.format("%.1f", info.progress)
            val status = info.status

            val contentText = when (status) {
                DownloadService.DownloadStatus.COMPLETED -> context.getString(R.string.download_complete)
                DownloadService.DownloadStatus.PAUSED -> "${context.getString(R.string.paused)} • $progressFormatted%"
                DownloadService.DownloadStatus.DOWNLOADING -> "${context.getString(R.string.downloading)} • $progressFormatted%"
                DownloadService.DownloadStatus.QUEUED -> context.getString(R.string.queued)
                DownloadService.DownloadStatus.FAILED -> context.getString(R.string.download_failed)
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(displayTitle)
                .setContentText(contentText)
                .setSmallIcon(R.drawable.void_icon)
                .setOngoing(
                    status != DownloadService.DownloadStatus.COMPLETED &&
                            status != DownloadService.DownloadStatus.FAILED
                )

            if (status != DownloadService.DownloadStatus.COMPLETED &&
                status != DownloadService.DownloadStatus.FAILED
            ) {
                
                builder.setProgress(100, progressInt, status == DownloadService.DownloadStatus.QUEUED)

                val pauseResumeText = when (status) {
                    DownloadService.DownloadStatus.DOWNLOADING -> context.getString(R.string.pause)
                    DownloadService.DownloadStatus.PAUSED -> context.getString(R.string.resume)
                    DownloadService.DownloadStatus.QUEUED -> context.getString(R.string.pause)
                    else -> context.getString(R.string.pause)
                }

                val pauseResumeIcon = when (status) {
                    DownloadService.DownloadStatus.DOWNLOADING -> android.R.drawable.ic_media_pause
                    DownloadService.DownloadStatus.PAUSED -> android.R.drawable.ic_media_play
                    DownloadService.DownloadStatus.QUEUED -> android.R.drawable.ic_media_pause
                    else -> android.R.drawable.ic_media_pause
                }

                builder.addAction(
                    pauseResumeIcon,
                    pauseResumeText,
                    actionIntent(context, ACTION_PAUSE_RESUME, mediaItem.id)
                )

                builder.addAction(
                    android.R.drawable.ic_delete,
                    context.getString(android.R.string.cancel),
                    actionIntent(context, ACTION_CANCEL, mediaItem.id)
                )
            } else {
                builder.setProgress(0, 0, false)
                builder.setAutoCancel(true)
                val file = info.file
                val uri = FileHelper.getContentUri(context, file)
                val mime = FileHelper.getMimeType(file) ?: "*/*"
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mime)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    "${mediaItem.id}_open".hashCode(),
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val downloadsIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val downloadsPendingIntent = PendingIntent.getActivity(
                    context,
                    "${mediaItem.id}_downloads".hashCode(),
                    downloadsIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                builder.setContentIntent(downloadsPendingIntent)

                builder.addAction(
                    android.R.drawable.ic_menu_view,
                    context.getString(R.string.open),
                    openPendingIntent
                )
            }

            
            val imageTag = if (mediaItem.type.equals("Episode", true)) {
                imageHelper.getBestImageTag(
                    null,
                    mediaItem.backdropImageTags
                ) ?: mediaItem.primaryImageTag
            } else {
                imageHelper.getBestImageTag(
                    mediaItem.primaryImageTag,
                    mediaItem.backdropImageTags
                )
            }

            val rawUrl = if (mediaItem.type.equals("Episode", true)) {
                imageHelper.createThumbnailUrl(info.serverUrl, mediaItem.id, imageTag)
            } else {
                imageHelper.createPosterUrl(info.serverUrl, mediaItem.id, imageTag)
            }

            val imageUrl = rawUrl?.let {
                if (info.accessToken.isNotBlank()) "$it&api_key=${info.accessToken}" else it
            }

            var bitmap: android.graphics.Bitmap? = null
            imageUrl?.let { url ->
                try {
                    val width = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
                    val height = context.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_height)
                    val request = ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .size(width, height)
                        .build()

                    val drawable = imageLoader.execute(request).drawable
                    bitmap = (drawable as? BitmapDrawable)?.bitmap

                    bitmap?.let {
                        builder.setLargeIcon(it)
                        if (status != DownloadService.DownloadStatus.COMPLETED &&
                            status != DownloadService.DownloadStatus.FAILED
                        ) {
                            builder.setStyle(NotificationCompat.BigPictureStyle().bigPicture(it))
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val notification = builder.build()
            bitmap?.recycle()
            notification
        }

        fun buildNotification(
            context: Context,
            info: DownloadService.DownloadInfo
        ): Notification {
            return buildBasicNotification(context, info)
        }

        private fun actionIntent(context: Context, action: String, id: String): PendingIntent {
            val intent = Intent(context, DownloadActionReceiver::class.java).apply {
                this.action = action
                putExtra(EXTRA_ID, id)
            }
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getBroadcast(context, (action + id).hashCode(), intent, flags)
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DownloadNotificationEntryPoint {
    fun imageHelper(): ImageHelper
}