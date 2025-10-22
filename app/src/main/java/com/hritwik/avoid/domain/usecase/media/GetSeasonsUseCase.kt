package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetSeasonsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetSeasonsUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val seriesId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getSeasons(
            userId = parameters.userId,
            seriesId = parameters.seriesId,
            accessToken = parameters.accessToken
        )
    }
}