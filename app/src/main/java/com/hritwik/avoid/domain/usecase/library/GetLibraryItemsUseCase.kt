package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class GetLibraryItemsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetLibraryItemsUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val libraryId: String,
        val accessToken: String,
        val startIndex: Int = 0,
        val limit: Int = 20,
        val forceRefresh: Boolean = false,
        val sortBy: List<String> = listOf("SortName"),
        val sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING,
        val genre: String? = null
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getLibraryItems(
            userId = parameters.userId,
            libraryId = parameters.libraryId,
            accessToken = parameters.accessToken,
            startIndex = parameters.startIndex,
            limit = parameters.limit,
            forceRefresh = parameters.forceRefresh,
            sortBy = parameters.sortBy,
            sortOrder = parameters.sortOrder,
            genre = parameters.genre
        )
    }
}
