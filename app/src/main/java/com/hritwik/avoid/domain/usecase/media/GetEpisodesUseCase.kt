package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetEpisodesUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetEpisodesUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val seasonId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getEpisodes(
            userId = parameters.userId,
            seasonId = parameters.seasonId,
            accessToken = parameters.accessToken
        )
    }
}