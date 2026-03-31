package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetWatchHistoryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetWatchHistoryUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String,
        val limit: Int = 50
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getWatchHistory(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            limit = parameters.limit
        )
    }
}
