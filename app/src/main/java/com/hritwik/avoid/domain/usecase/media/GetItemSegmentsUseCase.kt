package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.playback.Segment
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetItemSegmentsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetItemSegmentsUseCase.Params, List<Segment>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val mediaId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<Segment>> {
        return libraryRepository.getItemSegments(
            userId = parameters.userId,
            mediaId = parameters.mediaId,
            accessToken = parameters.accessToken
        )
    }
}