package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetSimilarItemsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetSimilarItemsUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val mediaId: String,
        val userId: String,
        val accessToken: String,
        val limit: Int = 10
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getSimilarItems(
            mediaId = parameters.mediaId,
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            limit = parameters.limit
        )
    }
}