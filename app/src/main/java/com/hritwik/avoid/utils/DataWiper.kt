package com.hritwik.avoid.utils

import android.content.Context
import com.hritwik.avoid.data.cache.CacheManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.download.DownloadService
import com.hritwik.avoid.data.local.database.VoidDatabase
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataWiper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val cacheManager: CacheManager
) {
    suspend fun wipeAll() = withContext(Dispatchers.IO) {
        preferencesManager.clearAllPlaybackPositions()
        val database = VoidDatabase.getDatabase(context)
        database.searchResultDao().clearAll()
        database.clearDatabase()

        cacheManager.clearAll()

        UserDataViewModel.deleteDir(context.cacheDir, "Cache directory")
        context.externalCacheDir?.let { UserDataViewModel.deleteDir(it, "External cache directory") }

        UserDataViewModel.deleteDir(File(context.filesDir, "downloads"), "Downloads directory")
        context.getExternalFilesDir(null)?.let { UserDataViewModel.deleteDir(File(it, "downloads"), "External downloads directory") }

        UserDataViewModel.deleteDir(File(context.cacheDir, "temp"), "Temp cache directory")
        UserDataViewModel.deleteDir(File(context.filesDir, "temp"), "Temp files directory")
        context.getExternalFilesDir(null)?.let { UserDataViewModel.deleteDir(File(it, "temp"), "External temp directory") }
    }
}
