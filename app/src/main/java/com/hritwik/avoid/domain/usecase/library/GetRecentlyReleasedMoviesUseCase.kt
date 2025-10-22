package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class GetRecentlyReleasedMoviesUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetRecentlyReleasedMoviesUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String,
        val limit: Int = 20
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getRecentlyReleasedMovies(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            limit = parameters.limit
        )
    }
}