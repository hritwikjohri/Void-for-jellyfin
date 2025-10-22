package com.hritwik.avoid.domain.usecase.library

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetCollectionItemsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetCollectionItemsUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String,
        val collectionId: String,
        val sortBy: List<String> = listOf("SortName"),
        val sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING,
        val startIndex: Int = 0,
        val limit: Int = 50
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getCollectionItems(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            collectionId = parameters.collectionId,
            sortBy = parameters.sortBy,
            sortOrder = parameters.sortOrder,
            startIndex = parameters.startIndex,
            limit = parameters.limit
        )
    }
}
