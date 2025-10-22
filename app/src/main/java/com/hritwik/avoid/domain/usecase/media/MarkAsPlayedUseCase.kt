package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class MarkAsPlayedUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<MarkAsPlayedUseCase.Params, Unit>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val mediaId: String,
        val accessToken: String,
        val isPlayed: Boolean
    )

    override suspend fun execute(parameters: Params): NetworkResult<Unit> {
        return libraryRepository.markAsPlayed(
            userId = parameters.userId,
            mediaId = parameters.mediaId,
            accessToken = parameters.accessToken,
            isPlayed = parameters.isPlayed
        )
    }
}