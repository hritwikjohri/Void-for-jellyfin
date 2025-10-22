package com.hritwik.avoid.data.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.core.ServiceManagerEntryPoint
import dagger.hilt.android.EntryPointAccessors

@OptIn(UnstableApi::class)
class DownloadActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val downloadId = intent.getStringExtra(DownloadNotificationService.EXTRA_ID) ?: return
        val serviceManager = EntryPointAccessors.fromApplication(
            context.applicationContext,
            ServiceManagerEntryPoint::class.java
        ).serviceManager()
        when (intent.action) {
            DownloadNotificationService.ACTION_PAUSE_RESUME -> {
                val currentDownload = serviceManager.downloads.value[downloadId]

                when (currentDownload?.status) {
                    DownloadService.DownloadStatus.DOWNLOADING,
                    DownloadService.DownloadStatus.QUEUED -> {
                        serviceManager.pauseDownload(downloadId)
                    }
                    DownloadService.DownloadStatus.PAUSED -> {
                        serviceManager.resumeDownload(downloadId)
                    }
                    else -> {
                        serviceManager.pauseDownload(downloadId)
                    }
                }
            }
            DownloadNotificationService.ACTION_CANCEL -> {
                serviceManager.cancelDownload(downloadId)
            }
        }
    }
}