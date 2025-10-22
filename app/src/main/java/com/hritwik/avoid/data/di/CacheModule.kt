package com.hritwik.avoid.data.di

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.hritwik.avoid.data.local.PreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CacheModule {

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideCacheEvictor(
        preferencesManager: PreferencesManager
    ): LeastRecentlyUsedCacheEvictor {
        val cacheSizeMb = runBlocking { preferencesManager.getVideoCacheSize().first() }
        val cacheSizeBytes = cacheSizeMb * 1024 * 1024
        return LeastRecentlyUsedCacheEvictor(cacheSizeBytes)
    }

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideSimpleCache(
        @ApplicationContext context: Context,
        cacheEvictor: LeastRecentlyUsedCacheEvictor
    ): SimpleCache {
        val cacheDir = File(context.cacheDir, "media_cache")
        return SimpleCache(
            cacheDir,
            cacheEvictor,
            StandaloneDatabaseProvider(context)
        )
    }

    @Provides
    @Singleton
    fun provideOkHttpDataSourceFactory(
        okHttpClient: OkHttpClient
    ): OkHttpDataSource.Factory = OkHttpDataSource.Factory(okHttpClient)

    @Provides
    @Singleton
    fun provideUpstreamFactory(
        @ApplicationContext context: Context,
        okHttpFactory: OkHttpDataSource.Factory
    ): DefaultDataSource.Factory = DefaultDataSource.Factory(context, okHttpFactory)

    @OptIn(UnstableApi::class)
    @Provides
    @Singleton
    fun provideCacheDataSourceFactory(
        simpleCache: SimpleCache,
        upstreamFactory: DefaultDataSource.Factory
    ): CacheDataSource.Factory {
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(upstreamFactory)
    }
}
