package com.hritwik.avoid.domain.usecase.activity

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.activity.ActivityLogResult
import com.hritwik.avoid.domain.repository.ActivityLogRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetActivityLogEntriesUseCase @Inject constructor(
    private val activityLogRepository: ActivityLogRepository
) : BaseUseCase<GetActivityLogEntriesUseCase.Params, ActivityLogResult>(Dispatchers.IO) {

    data class Params(
        val accessToken: String,
        val startIndex: Int = 0,
        val limit: Int = 100,
        val minDate: String? = null,
        val hasUserId: Boolean? = null
    )

    override suspend fun execute(parameters: Params): NetworkResult<ActivityLogResult> {
        return activityLogRepository.getActivityLogEntries(
            accessToken = parameters.accessToken,
            startIndex = parameters.startIndex,
            limit = parameters.limit,
            minDate = parameters.minDate,
            hasUserId = parameters.hasUserId
        )
    }
}
