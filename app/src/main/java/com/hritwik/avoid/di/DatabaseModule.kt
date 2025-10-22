package com.hritwik.avoid.di

import android.content.Context
import com.hritwik.avoid.data.local.database.VoidDatabase
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.dao.LibraryDao
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.database.dao.PendingActionDao
import com.hritwik.avoid.data.local.database.dao.PlaybackLogDao
import com.hritwik.avoid.data.local.database.dao.SearchResultDao
import com.hritwik.avoid.data.local.database.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVoidDatabase(
        @ApplicationContext context: Context
    ): VoidDatabase {
        return VoidDatabase.getDatabase(context)
    }

    @Provides
    fun provideMediaItemDao(database: VoidDatabase): MediaItemDao {
        return database.mediaItemDao()
    }

    @Provides
    fun provideLibraryDao(database: VoidDatabase): LibraryDao {
        return database.libraryDao()
    }

    @Provides
    fun provideUserDao(database: VoidDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun provideDownloadDao(database: VoidDatabase): DownloadDao {
        return database.downloadDao()
    }

    @Provides
    fun providePlaybackLogDao(database: VoidDatabase): PlaybackLogDao {
        return database.playbackLogDao()
    }

    @Provides
    fun provideSearchResultDao(database: VoidDatabase): SearchResultDao {
        return database.searchResultDao()
    }

    @Provides
    fun providePendingActionDao(database: VoidDatabase): PendingActionDao {
        return database.pendingActionDao()
    }

}