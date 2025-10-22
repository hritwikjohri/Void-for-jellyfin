package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class MarkSeasonAsPlayedUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<MarkSeasonAsPlayedUseCase.Params, Unit>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val seasonId: String,
        val accessToken: String,
        val isPlayed: Boolean
    )

    override suspend fun execute(parameters: Params): NetworkResult<Unit> {
        
        return when (val episodesResult = libraryRepository.getEpisodes(
            userId = parameters.userId,
            seasonId = parameters.seasonId,
            accessToken = parameters.accessToken
        )) {
            is NetworkResult.Success -> {
                
                val episodes = episodesResult.data
                var hasError = false
                var errorMessage = ""

                episodes.forEach { episode ->
                    when (val result = libraryRepository.markAsPlayed(
                        userId = parameters.userId,
                        mediaId = episode.id,
                        accessToken = parameters.accessToken,
                        isPlayed = parameters.isPlayed
                    )) {
                        is NetworkResult.Error -> {
                            hasError = true
                            errorMessage = result.message
                        }
                        else -> { }
                    }
                }

                if (hasError) {
                    NetworkResult.Error<Unit>(AppError.Unknown("Failed to mark some episodes: $errorMessage"))
                } else {
                    NetworkResult.Success(Unit)
                }
            }
            is NetworkResult.Error -> NetworkResult.Error<Unit>(
                AppError.Unknown("Failed to get episodes: ${episodesResult.message}")
            )
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }
}