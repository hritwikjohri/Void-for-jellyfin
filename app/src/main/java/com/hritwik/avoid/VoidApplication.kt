package com.hritwik.avoid

import android.app.Application
import android.content.ComponentCallbacks2
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.hritwik.avoid.core.ServiceManager
import com.hritwik.avoid.data.cache.CacheManager
import com.hritwik.avoid.data.download.DownloadService
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.PendingActionDao
import com.hritwik.avoid.data.network.CdnInterceptor
import com.hritwik.avoid.data.sync.UserDataSyncWorker
import com.hritwik.avoid.di.ApplicationScope
import com.hritwik.avoid.utils.CrashReporter
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import com.hritwik.avoid.data.sync.PlaybackLogSyncWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.Locale
import javax.inject.Inject
import okhttp3.OkHttpClient

@HiltAndroidApp
class VoidApplication : Application() {
    @Inject
    lateinit var preferencesManager: PreferencesManager
    lateinit var imageLoader: ImageLoader
    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope
    @Inject
    lateinit var serviceManager: ServiceManager
    @Inject
    lateinit var networkMonitor: NetworkMonitor
    @Inject
    lateinit var pendingActionDao: PendingActionDao
    @Inject
    lateinit var cacheManager: CacheManager
    @Inject
    lateinit var okHttpClient: OkHttpClient

    private val trimCallbacks = mutableListOf<ComponentCallbacks2>()

    private val crashHandler = CoroutineExceptionHandler { _, throwable ->
        CrashReporter.report(throwable)
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        copyBundledFonts()
        applyTheme()
        val coilOkHttpClient = okHttpClient.newBuilder()
            .apply { interceptors().removeAll { it is CdnInterceptor } }
            .build()
        imageLoader = ImageLoader.Builder(this)
            .okHttpClient(coilOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(cacheDir, "coil_image_cache"))
                    .maxSizePercent(0.02)
                    .build()
            }
            .build()
        Coil.setImageLoader(imageLoader)
        initializePreferences()
        if (isIgnoringBatteryOptimizations()) {
            applicationScope.launch(crashHandler) {
                val syncEnabled = preferencesManager.getSyncEnabled().first()
                if (syncEnabled) {
                    PlaybackLogSyncWorker.enqueue(this@VoidApplication)
                }
                serviceManager.register(DownloadService::class)
                serviceManager.cleanupOrphanedDownloads()
            }
        } else {
            requestIgnoreBatteryOptimizations()
        }

        applicationScope.launch(crashHandler) {
            networkMonitor.isConnected.collect { connected ->
                if (connected && pendingActionDao.getPendingActions().isNotEmpty()) {
                    UserDataSyncWorker.syncNow(this@VoidApplication)
                }
            }
        }

        registerComponentCallbacks(object : ComponentCallbacks2 {
            override fun onTrimMemory(level: Int) {
                trimCallbacks.forEach { it.onTrimMemory(level) }
            }

            override fun onConfigurationChanged(newConfig: android.content.res.Configuration) = Unit

            override fun onLowMemory() {
                trimCallbacks.forEach { it.onLowMemory() }
            }
        })
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = getSystemService(PowerManager::class.java)
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestIgnoreBatteryOptimizations() {
        val intent = Intent(ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = "package:$packageName".toUri()
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    private fun applyTheme() {
        val mode = runBlocking { preferencesManager.getThemeMode().first() }
        val nightMode = if (mode == "light")
            AppCompatDelegate.MODE_NIGHT_NO
        else
            AppCompatDelegate.MODE_NIGHT_YES
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    private fun copyBundledFonts() {
        val fontsDir = File(filesDir, "fonts").apply { if (!exists()) mkdirs() }
        val fonts = listOf(
            R.font.noto_sans to "NotoSans-Regular.ttf",
            R.font.noto_sans_arabic to "NotoSans-Arabic.ttf",
            R.font.noto_sans_devanagari to "NotoSans-Devanagari.ttf",
            R.font.noto_sans_variable to "NotoSans-Variable.ttf",
            R.font.source_sans_variable to "SourceSans-Variable.ttf",
            R.font.source_sans_italic to "SourceSans-Italic.ttf",
        )
        fonts.forEach { (resId, name) ->
            val outFile = File(fontsDir, name)
            if (!outFile.exists()) {
                resources.openRawResource(resId).use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }

    private fun initializePreferences() {
        applicationScope.launch(crashHandler) {
            preferencesManager.initializeThemePreferences()
            val lang = preferencesManager.getPreferredLanguage().first()
            applyPersonalization(lang)
        }
    }

    private fun applyPersonalization(language: String) {
        val config = resources.configuration
        config.setLocale(Locale(language))
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun registerTrimMemoryCallback(callback: ComponentCallbacks2) {
        trimCallbacks += callback
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        imageLoader.memoryCache?.trimMemory(level)
        if (level >= TRIM_MEMORY_BACKGROUND) {
            cacheManager.clearAll()
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
       imageLoader.memoryCache?.clear()
       cacheManager.clearAll()
    }
}
