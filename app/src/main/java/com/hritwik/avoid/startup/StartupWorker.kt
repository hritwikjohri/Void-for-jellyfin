package com.hritwik.avoid.startup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hritwik.avoid.data.prefetch.PrefetchManager
import com.hritwik.avoid.data.prefetch.PrefetchWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Startup worker that initializes prefetching mechanisms on app launch.
 *
 * Responsibilities:
 * 1. Start PrefetchManager to watch and prefetch next episodes for series in progress
 * 2. Enqueue PrefetchWorker to immediately fetch and cache next up episodes
 */
class StartupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StartupWorkerEntryPoint {
        fun prefetchManager(): PrefetchManager
    }

    override suspend fun doWork(): Result {
        // No-op to avoid startup network load
        return Result.success()
    }
}
