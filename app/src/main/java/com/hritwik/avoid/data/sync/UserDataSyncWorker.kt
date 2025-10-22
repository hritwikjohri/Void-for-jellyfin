package com.hritwik.avoid.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.database.dao.PendingActionDao
import com.hritwik.avoid.domain.repository.LibraryRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class UserDataSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters,

) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncWorkerEntryPoint {
        fun pendingActionDao(): PendingActionDao
        fun preferencesManager(): PreferencesManager
        fun libraryRepository(): LibraryRepository
        fun mediaItemDao(): MediaItemDao
    }

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            SyncWorkerEntryPoint::class.java
        )
        val actionDao = entryPoint.pendingActionDao()
        val prefs = entryPoint.preferencesManager()
        val repository = entryPoint.libraryRepository()
        val mediaDao = entryPoint.mediaItemDao()

        val userId = prefs.getUserId().first() ?: return Result.success()
        val token = prefs.getAccessToken().first() ?: return Result.success()

        val start = System.currentTimeMillis()
        var processed = 0L
        val actions = actionDao.getPendingActions()
        for (action in actions) {
            if (action.actionType == "watchlist") {
                mediaDao.getMediaItem(action.mediaId, userId)?.let {
                    mediaDao.updateMediaItem(it.copy(pendingWatchlist = false))
                }
                actionDao.deleteAction(action.mediaId, action.actionType)
                processed++
                continue
            }

            val currentItem = mediaDao.getMediaItem(action.mediaId, userId)
            val result = when (action.actionType) {
                "favorite" -> repository.updateFavoriteRemote(userId, action.mediaId, token, action.newValue)
                "played" -> repository.updatePlayedRemote(userId, action.mediaId, token, action.newValue)
                else -> NetworkResult.Success(Unit)
            }
            if (result is NetworkResult.Success) {
                when (action.actionType) {
                    "favorite" -> currentItem?.let {
                        mediaDao.updateMediaItem(it.copy(isFavorite = action.newValue, pendingFavorite = false))
                    }
                    "played" -> currentItem?.let {
                        mediaDao.updateMediaItem(
                            it.copy(
                                played = action.newValue,
                                pendingPlayed = false
                            )
                        )
                    }
                }
                processed++
            } else {
                currentItem?.let {
                    when (action.actionType) {
                        "favorite" -> mediaDao.updateMediaItem(
                            it.copy(isFavorite = !action.newValue, pendingFavorite = false)
                        )
                        "played" -> mediaDao.updateMediaItem(
                            it.copy(
                                played = !action.newValue,
                                playCount = (it.playCount - 1).coerceAtLeast(0),
                                pendingPlayed = false
                            )
                        )
                    }
                }
                return Result.retry()
            }
        }
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "UserDataSync"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<UserDataSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .setRequiresCharging(true)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        fun syncNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<UserDataSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
