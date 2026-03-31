package com.hritwik.avoid.di

import com.hritwik.avoid.data.repository.ContinueWatchingStore
import com.hritwik.avoid.data.repository.NextUpStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideContinueWatchingStore(): ContinueWatchingStore = ContinueWatchingStore()

    @Provides
    @Singleton
    fun provideNextUpStore(): NextUpStore = NextUpStore()
}
