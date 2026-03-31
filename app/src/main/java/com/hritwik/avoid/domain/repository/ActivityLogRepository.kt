package com.hritwik.avoid.domain.repository

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.activity.ActivityLogResult

interface ActivityLogRepository {
    suspend fun getActivityLogEntries(
        accessToken: String,
        startIndex: Int = 0,
        limit: Int = 100,
        minDate: String? = null,
        hasUserId: Boolean? = null
    ): NetworkResult<ActivityLogResult>
}
