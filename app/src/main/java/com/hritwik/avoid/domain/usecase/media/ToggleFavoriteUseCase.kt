package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.model.library.MediaItem
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<ToggleFavoriteUseCase.Params, Unit>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val mediaId: String,
        val accessToken: String,
        val isFavorite: Boolean,
        val mediaItem: MediaItem? = null
    )

    override suspend fun execute(parameters: Params): NetworkResult<Unit> {
        return libraryRepository.toggleFavorite(
            userId = parameters.userId,
            mediaId = parameters.mediaId,
            accessToken = parameters.accessToken,
            newFavorite = parameters.isFavorite,
            mediaItem = parameters.mediaItem
        )
    }
}
