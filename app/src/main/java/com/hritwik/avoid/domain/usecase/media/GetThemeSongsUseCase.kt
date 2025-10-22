package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetThemeSongsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetThemeSongsUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val mediaId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getThemeSongs(
            mediaId = parameters.mediaId,
            accessToken = parameters.accessToken
        )
    }
}
