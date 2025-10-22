package com.hritwik.avoid.domain.usecase.jellyseer

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaDetail
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.domain.repository.JellyseerRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetJellyseerMediaDetailsUseCase @Inject constructor(
    private val repository: JellyseerRepository
) : BaseUseCase<GetJellyseerMediaDetailsUseCase.Params, JellyseerMediaDetail>(Dispatchers.IO) {

    data class Params(
        val id: Long,
        val type: JellyseerMediaType,
        val language: String? = null
    )

    override suspend fun execute(parameters: Params): NetworkResult<JellyseerMediaDetail> {
        return repository.getMediaDetails(
            id = parameters.id,
            type = parameters.type,
            language = parameters.language
        )
    }
}
