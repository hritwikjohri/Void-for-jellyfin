package com.hritwik.avoid.domain.usecase.media

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.Person
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetMediaCreditsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetMediaCreditsUseCase.Params, List<Person>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val mediaId: String,
        val accessToken: String
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<Person>> {
        return libraryRepository.getMediaCredits(
            userId = parameters.userId,
            mediaId = parameters.mediaId,
            accessToken = parameters.accessToken
        )
    }
}
