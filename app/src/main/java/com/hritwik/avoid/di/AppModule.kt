package com.hritwik.avoid.di

import android.content.Context
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.network.CdnInterceptor
import com.hritwik.avoid.data.network.LocalNetworkSslHelper
import com.hritwik.avoid.data.network.MtlsCertificateProvider
import com.hritwik.avoid.data.network.OfflineCacheInterceptor
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import com.hritwik.avoid.utils.helpers.NetworkMonitor
import com.hritwik.avoid.utils.constants.AppConstants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.ConnectionPool
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class DownloadClient

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        loggingInterceptor: HttpLoggingInterceptor,
        cdnInterceptor: CdnInterceptor,
        offlineCacheInterceptor: OfflineCacheInterceptor,
        connectionPool: ConnectionPool,
        mtlsCertificateProvider: MtlsCertificateProvider,
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, AppConstants.HTTP_CACHE_DIR)
        val cache = Cache(cacheDir, AppConstants.HTTP_CACHE_SIZE)
        val sslConfig = LocalNetworkSslHelper.createSslConfig(mtlsCertificateProvider.keyManager())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslConfig.sslSocketFactory, sslConfig.trustManager)
            .hostnameVerifier(sslConfig.hostnameVerifier)
            .cache(cache)
            .connectionPool(connectionPool)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header(
                        "Cache-Control",
                        response.header("Cache-Control") ?: "public, max-age=60"
                    )
                    .removeHeader("Pragma")
                    .build()
            }
            .addInterceptor(offlineCacheInterceptor)
            .addInterceptor(cdnInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(AppConstants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @DownloadClient
    fun provideDownloadOkHttpClient(
        @ApplicationContext context: Context,
        cdnInterceptor: CdnInterceptor,
        offlineCacheInterceptor: OfflineCacheInterceptor,
        connectionPool: ConnectionPool,
        mtlsCertificateProvider: MtlsCertificateProvider,
    ): OkHttpClient {
        val cacheDir = File(context.cacheDir, AppConstants.HTTP_CACHE_DIR)
        val cache = Cache(cacheDir, AppConstants.HTTP_CACHE_SIZE)
        val sslConfig = LocalNetworkSslHelper.createSslConfig(mtlsCertificateProvider.keyManager())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslConfig.sslSocketFactory, sslConfig.trustManager)
            .hostnameVerifier(sslConfig.hostnameVerifier)
            .cache(cache)
            .connectionPool(connectionPool)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                response.newBuilder()
                    .header(
                        "Cache-Control",
                        response.header("Cache-Control") ?: "public, max-age=60"
                    )
                    .removeHeader("Pragma")
                    .build()
            }
            .addInterceptor(offlineCacheInterceptor)
            .addInterceptor(cdnInterceptor)
            .connectTimeout(AppConstants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(AppConstants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(AppConstants.NETWORK_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideConnectionPool(): ConnectionPool {
        return ConnectionPool(5, 5, TimeUnit.MINUTES)
    }

    @Provides
    @Singleton
    fun providePreferencesManager(
        @ApplicationContext context: Context
    ): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun providePriorityDispatcher(
        okHttpClient: OkHttpClient,
    ): PriorityDispatcher = PriorityDispatcher(okHttpClient.dispatcher)

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor = NetworkMonitor(context)

    @Provides
    @Singleton
    fun provideConnectivityObserver(
        monitor: NetworkMonitor
    ): ConnectivityObserver = monitor
}
