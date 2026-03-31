package com.hritwik.avoid.data.repository

import android.content.Context
import android.provider.Settings.Secure.ANDROID_ID
import com.hritwik.avoid.data.common.BaseRepository
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.connection.ServerConnectionManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.network.PriorityDispatcher
import com.hritwik.avoid.data.remote.JellyfinApiService
import com.hritwik.avoid.domain.mapper.ActivityLogMapper
import com.hritwik.avoid.domain.model.activity.ActivityLogResult
import com.hritwik.avoid.domain.repository.ActivityLogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import retrofit2.Retrofit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityLogRepositoryImpl @Inject constructor(
    private val retrofitBuilder: Retrofit.Builder,
    private val preferencesManager: PreferencesManager,
    private val activityLogMapper: ActivityLogMapper,
    private val serverConnectionManager: ServerConnectionManager,
    @ApplicationContext private val context: Context,
    priorityDispatcher: PriorityDispatcher
) : BaseRepository(priorityDispatcher, serverConnectionManager), ActivityLogRepository {

    private val deviceId: String by lazy {
        android.provider.Settings.Secure.getString(context.contentResolver, ANDROID_ID) ?: "unknown"
    }

    private fun createApiService(serverUrl: String): JellyfinApiService {
        val baseUrl = if (!serverUrl.endsWith("/")) "$serverUrl/" else serverUrl
        return retrofitBuilder
            .baseUrl(baseUrl)
            .build()
            .create(JellyfinApiService::class.java)
    }

    private suspend fun getServerUrl(): String {
        return preferencesManager.getServerUrl().first()
            ?: throw IllegalStateException("No server URL found")
    }

    override suspend fun getActivityLogEntries(
        accessToken: String,
        startIndex: Int,
        limit: Int,
        minDate: String?,
        hasUserId: Boolean?
    ): NetworkResult<ActivityLogResult> {
        val serverUrl = getServerUrl()
        return safeApiCall(serverUrl) {
            val apiService = createApiService(serverUrl)
            val authHeader = JellyfinApiService.createAuthHeader(deviceId, token = accessToken)

            val response = apiService.getActivityLogEntries(
                startIndex = startIndex,
                limit = limit,
                minDate = minDate,
                hasUserId = hasUserId,
                authorization = authHeader
            )

            activityLogMapper.mapActivityLogQueryResult(response)
        }
    }
}
