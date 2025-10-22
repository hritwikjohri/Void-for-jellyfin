package com.hritwik.avoid.domain.repository

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerConfig
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaDetail
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.domain.model.jellyseer.JellyseerPagedResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerRequest
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerUser
import kotlinx.coroutines.flow.Flow

interface JellyseerRepository {
    fun jellyseerConfig(): Flow<JellyseerConfig>

    suspend fun search(
        query: String,
        page: Int = 1,
        language: String? = null
    ): NetworkResult<JellyseerPagedResult<JellyseerSearchResult>>

    suspend fun getMediaDetails(
        id: Long,
        type: JellyseerMediaType,
        language: String? = null
    ): NetworkResult<JellyseerMediaDetail>

    suspend fun createRequest(
        id: Long,
        type: JellyseerMediaType,
        is4k: Boolean = false,
        seasons: List<Int>? = null,
        tvdbId: Long? = null
    ): NetworkResult<JellyseerRequest>

    suspend fun loginWithJellyfin(
        baseUrl: String,
        username: String,
        password: String,
    ): NetworkResult<JellyseerUser>

    suspend fun logout(): NetworkResult<Unit>
}
