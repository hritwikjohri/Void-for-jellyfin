package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetThemeSongIdsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetThemeSongIdsUseCase.Params, List<String>>(Dispatchers.IO) {

    data class Params(
        val mediaId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<String>> {
        return libraryRepository.getThemeSongIds(
            mediaId = parameters.mediaId,
            accessToken = parameters.accessToken
        )
    }
}
