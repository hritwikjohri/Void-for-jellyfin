package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetPersonAppearancesUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetPersonAppearancesUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val personId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getPersonAppearances(
            userId = parameters.userId,
            personId = parameters.personId,
            accessToken = parameters.accessToken
        )
    }
}
