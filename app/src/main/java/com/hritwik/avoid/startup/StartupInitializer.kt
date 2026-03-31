package com.hritwik.avoid.startup

import android.content.Context
import androidx.startup.Initializer
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkManagerInitializer
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.sync.UserDataSyncWorker
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class StartupInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // Cancel any pending background work from previous installs to avoid startup network load
        WorkManager.getInstance(context).cancelAllWork()

        // Skip StartupWorker to avoid extra network load during app launch

        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            StartupEntryPoint::class.java
        )
        CoroutineScope(Dispatchers.Default).launch {
            // Removed ensureActiveConnection() - ServerConnectionManager automatically
            // handles connection state changes via its init block. No need to proactively
            // ping servers on app startup (saves up to 5 seconds).
            val syncEnabled = entryPoint.preferencesManager().getSyncEnabled().first()
            if (syncEnabled) {
                UserDataSyncWorker.enqueue(context)
            } else {
                UserDataSyncWorker.cancel(context)
            }
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = listOf(WorkManagerInitializer::class.java)

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StartupEntryPoint {
        fun preferencesManager(): PreferencesManager
        fun serverConnectionManager(): ServerConnectionManager
    }
}
