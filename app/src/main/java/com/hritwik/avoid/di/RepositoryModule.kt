package com.hritwik.avoid.di

import com.hritwik.avoid.data.repository.AuthRepositoryImpl
import com.hritwik.avoid.data.repository.DownloadRepositoryImpl
import com.hritwik.avoid.data.repository.JellyseerRepositoryImpl
import com.hritwik.avoid.data.repository.LibraryRepositoryImpl
import com.hritwik.avoid.data.repository.SearchRepositoryImpl
import com.hritwik.avoid.domain.repository.AuthRepository
import com.hritwik.avoid.domain.repository.DownloadRepository
import com.hritwik.avoid.domain.repository.JellyseerRepository
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.repository.SearchRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindLibraryRepository(
        libraryRepositoryImpl: LibraryRepositoryImpl
    ): LibraryRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(
        searchRepositoryImpl: SearchRepositoryImpl
    ): SearchRepository

    @Binds
    @Singleton
    abstract fun bindDownloadRepository(
        downloadRepositoryImpl: DownloadRepositoryImpl
    ): DownloadRepository

    @Binds
    @Singleton
    abstract fun bindJellyseerRepository(
        jellyseerRepositoryImpl: JellyseerRepositoryImpl
    ): JellyseerRepository
}
