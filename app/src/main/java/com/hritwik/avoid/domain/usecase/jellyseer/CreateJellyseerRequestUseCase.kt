package com.hritwik.avoid.domain.usecase.jellyseer

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.domain.model.jellyseer.JellyseerRequest
import com.hritwik.avoid.domain.repository.JellyseerRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class CreateJellyseerRequestUseCase @Inject constructor(
    private val repository: JellyseerRepository
) : BaseUseCase<CreateJellyseerRequestUseCase.Params, JellyseerRequest>(Dispatchers.IO) {

    data class Params(
        val id: Long,
        val type: JellyseerMediaType,
        val is4k: Boolean = false,
        val seasons: List<Int>? = null,
        val tvdbId: Long? = null
    )

    override suspend fun execute(parameters: Params): NetworkResult<JellyseerRequest> {
        return repository.createRequest(
            id = parameters.id,
            type = parameters.type,
            is4k = parameters.is4k,
            seasons = parameters.seasons,
            tvdbId = parameters.tvdbId
        )
    }
}
