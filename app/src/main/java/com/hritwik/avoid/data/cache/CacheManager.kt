package com.hritwik.avoid.data.cache

import android.content.Context
import android.util.Log
import android.util.LruCache
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.SimpleCache
import coil.Coil
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.CrashReporter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @OptIn(UnstableApi::class)
@Inject constructor(
    private val simpleCache: SimpleCache,
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager
) {
    private val imageLoader get() = Coil.imageLoader(context)

    private val metadataCache = object : LruCache<String, MediaItem>(METADATA_CACHE_LIMIT) {}
    private val posterCache = object : LruCache<String, ByteArray>(POSTER_CACHE_LIMIT_KB) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size / 1024
    }
    @OptIn(UnstableApi::class)
    fun getVideoCacheUsage(): Long = simpleCache.cacheSpace

    fun getImageCacheUsage(): Long = imageLoader.diskCache?.size ?: 0L

    @OptIn(UnstableApi::class)
    fun clearVideoCache() {
        try {
            simpleCache.cacheSpace
            Log.d(TAG, "Video cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear video cache", e)
            CrashReporter.report(e)
        }
    }

    fun clearImageCache() {
        imageLoader.diskCache?.clear()
    }

    fun clearPosterCache() {
        posterCache.evictAll()
    }

    fun clearMetadataCache() {
        metadataCache.evictAll()
    }

    fun clearAll() {
        clearVideoCache()
        clearImageCache()
        clearPosterCache()
        clearMetadataCache()
    }

    @OptIn(UnstableApi::class)
    suspend fun setVideoCacheSize(sizeMb: Long) {
        preferencesManager.saveVideoCacheSize(sizeMb)
    }

    suspend fun setImageCacheSize(sizeMb: Long) {
        preferencesManager.saveImageCacheSize(sizeMb)
    }

    fun putMetadata(item: MediaItem) {
        metadataCache.put(item.id, item)
    }

    fun getMetadata(id: String): MediaItem? = metadataCache.get(id)

    fun putPoster(id: String, data: ByteArray) {
        posterCache.put(id, data)
    }

    fun getPoster(id: String): ByteArray? = posterCache.get(id)

    companion object {
        private const val TAG = "CacheManager"
        private const val METADATA_CACHE_LIMIT = 50
        private const val POSTER_CACHE_LIMIT_KB = 5 * 1024 
    }
}
