package com.hritwik.avoid.utils.helpers

import android.content.Context
import android.text.format.Formatter
import com.hritwik.avoid.R
import com.hritwik.avoid.data.download.DownloadService
import java.util.Locale

object DownloadDisplayFormatter {

    private const val PROGRESS_DECIMALS = "%.1f"

    fun formatStatus(context: Context, info: DownloadService.DownloadInfo): String {
        val status = info.status
        val progressFormatted = String.format(Locale.getDefault(), PROGRESS_DECIMALS, info.progress)
        val downloadedText = info.downloadedBytes.takeIf { it > 0 }?.let { formatBytes(context, it) }
        val totalText = info.totalBytes?.takeIf { it > 0 }?.let { formatBytes(context, it) }
        return when (status) {
            DownloadService.DownloadStatus.DOWNLOADING -> {
                if (!info.request.static) {
                    downloadedText?.let {
                        context.getString(R.string.download_transcoding_with_size, it)
                    } ?: context.getString(R.string.transcoding)
                } else if (downloadedText != null && totalText != null && info.progress > 0f) {
                    context.getString(
                        R.string.download_downloading_with_total,
                        downloadedText,
                        totalText,
                        progressFormatted
                    )
                } else if (downloadedText != null) {
                    context.getString(R.string.download_downloading_partial, downloadedText)
                } else {
                    context.getString(R.string.downloading)
                }
            }

            DownloadService.DownloadStatus.PAUSED -> {
                if (downloadedText != null && totalText != null && info.progress > 0f) {
                    context.getString(
                        R.string.download_paused_with_total,
                        downloadedText,
                        totalText,
                        progressFormatted
                    )
                } else if (downloadedText != null) {
                    context.getString(R.string.download_paused_partial, downloadedText)
                } else {
                    context.getString(R.string.paused)
                }
            }

            DownloadService.DownloadStatus.QUEUED -> context.getString(R.string.queued)
            DownloadService.DownloadStatus.COMPLETED -> context.getString(R.string.download_complete)
            DownloadService.DownloadStatus.FAILED -> context.getString(R.string.download_failed)
        }
    }

    fun progressFraction(info: DownloadService.DownloadInfo): Float? {
        return if (info.request.static && info.progress > 0f) {
            (info.progress.coerceIn(0f, 100f)) / 100f
        } else {
            null
        }
    }

    fun isProgressIndeterminate(info: DownloadService.DownloadInfo): Boolean {
        return info.status == DownloadService.DownloadStatus.DOWNLOADING && !info.request.static
    }

    private fun formatBytes(context: Context, bytes: Long): String {
        return Formatter.formatFileSize(context, bytes.coerceAtLeast(0L))
    }
}
