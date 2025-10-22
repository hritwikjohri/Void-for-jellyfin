package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetLibraryGenresUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetLibraryGenresUseCase.Params, List<String>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val libraryId: String,
        val accessToken: String,
        val sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<String>> {
        return libraryRepository.getLibraryGenres(
            userId = parameters.userId,
            libraryId = parameters.libraryId,
            accessToken = parameters.accessToken,
            sortOrder = parameters.sortOrder
        )
    }
}
