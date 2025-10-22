package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.repository.RelatedResources
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetRelatedResourcesBatchUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetRelatedResourcesBatchUseCase.Params, RelatedResources>(Dispatchers.IO) {

    data class Params(
        val mediaId: String,
        val userId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<RelatedResources> {
        return libraryRepository.getRelatedResourcesBatch(
            mediaId = parameters.mediaId,
            userId = parameters.userId,
            accessToken = parameters.accessToken
        )
    }
}
