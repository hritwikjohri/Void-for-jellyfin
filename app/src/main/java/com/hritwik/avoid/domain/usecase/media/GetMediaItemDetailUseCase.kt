package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetMediaItemDetailUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetMediaItemDetailUseCase.Params, MediaItem>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val mediaId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<MediaItem> {
        return libraryRepository.getMediaItemDetail(
            userId = parameters.userId,
            mediaId = parameters.mediaId,
            accessToken = parameters.accessToken
        )
    }
}