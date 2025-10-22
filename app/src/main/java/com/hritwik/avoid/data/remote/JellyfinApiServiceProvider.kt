package com.hritwik.avoid.data.remote

import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class JellyfinApiServiceProvider @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder
) {
    @Volatile
    private var currentBaseUrl: String? = null
    @Volatile
    private var apiService: JellyfinApiService? = null

    @Synchronized
    fun getApiService(serverUrl: String): JellyfinApiService {
        val baseUrl = if (!serverUrl.endsWith("/")) "$serverUrl/" else serverUrl
        if (apiService == null || currentBaseUrl != baseUrl) {
            apiService = retrofitBuilder
                .baseUrl(baseUrl)
                .build()
                .create(JellyfinApiService::class.java)
            currentBaseUrl = baseUrl
        }
        return apiService!!
    }
}
