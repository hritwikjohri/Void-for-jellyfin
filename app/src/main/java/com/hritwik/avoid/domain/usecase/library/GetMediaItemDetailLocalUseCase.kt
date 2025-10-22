package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import javax.inject.Inject

class GetMediaItemDetailLocalUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) {
    data class Params(
        val userId: String,
        val mediaId: String
    )

    suspend operator fun invoke(params: Params): MediaItem? {
        return libraryRepository.getMediaItemDetailLocal(
            userId = params.userId,
            mediaId = params.mediaId
        )
    }
}