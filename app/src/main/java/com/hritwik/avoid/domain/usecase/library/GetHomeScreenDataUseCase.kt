package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.HomeScreenData
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetHomeScreenDataUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetHomeScreenDataUseCase.Params, HomeScreenData>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String,
        val sectionLimit: Int = 10  // Reduced from 20 to 10 for better initial load performance
    )

    override suspend fun execute(parameters: Params): NetworkResult<HomeScreenData> {
        return libraryRepository.getHomeScreenData(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            limit = parameters.sectionLimit
        )
    }
}
