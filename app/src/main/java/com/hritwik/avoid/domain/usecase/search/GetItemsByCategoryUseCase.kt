package com.hritwik.avoid.domain.usecase.search

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class GetItemsByCategoryUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<GetItemsByCategoryUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String,
        val filters: Map<String, String>,
        val sortBy: List<String> = listOf("SortName"),
        val sortOrder: LibrarySortDirection = LibrarySortDirection.ASCENDING,
        val startIndex: Int = 0,
        val limit: Int = 50
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.getItemsByCategory(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            filters = parameters.filters,
            sortBy = parameters.sortBy,
            sortOrder = parameters.sortOrder,
            startIndex = parameters.startIndex,
            limit = parameters.limit
        )
    }
}