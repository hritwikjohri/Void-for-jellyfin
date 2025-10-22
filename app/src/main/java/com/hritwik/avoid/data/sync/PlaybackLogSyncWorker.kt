package com.hritwik.avoid.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.database.dao.PlaybackLogDao
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.repository.LibraryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first


class PlaybackLogSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun playbackLogDao(): PlaybackLogDao
        fun preferencesManager(): PreferencesManager
        fun libraryRepository(): LibraryRepository
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val playbackLogDao = entryPoint.playbackLogDao()
        val prefs = entryPoint.preferencesManager()
        val repository = entryPoint.libraryRepository()

        val userId = prefs.getUserId().first() ?: return Result.success()
        val token = prefs.getAccessToken().first() ?: return Result.success()

        val start = System.currentTimeMillis()
        var processed = 0L
        val logs = playbackLogDao.getUnSyncedLogs()
        for (log in logs) {
            val result = repository.reportPlaybackStop(
                mediaId = log.mediaId,
                userId = userId,
                accessToken = token,
                positionTicks = log.positionTicks
            )
            if (result is NetworkResult.Success) {
                playbackLogDao.markAsSynced(log.mediaId)
                processed++
            } else {
                return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "PlaybackLogSync"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<PlaybackLogSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}