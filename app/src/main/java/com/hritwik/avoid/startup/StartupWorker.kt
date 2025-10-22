package com.hritwik.avoid.startup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.hritwik.avoid.data.prefetch.PrefetchManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

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
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            StartupWorkerEntryPoint::class.java
        )
        entryPoint.prefetchManager().start()
        return Result.success()
    }
}
