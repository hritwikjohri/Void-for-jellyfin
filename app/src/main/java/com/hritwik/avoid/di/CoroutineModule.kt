package com.hritwik.avoid.di

import com.hritwik.avoid.data.network.MtlsCertificateProvider
import javax.inject.Qualifier
import javax.inject.Singleton
import dagger.Module
import dagger.Provides
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class WebSocketScope

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {
    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Provides
    @Singleton
    @WebSocketScope
    fun provideWebSocketScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PlayerNetworkEntryPoint {
    fun mtlsCertificateProvider(): MtlsCertificateProvider
}