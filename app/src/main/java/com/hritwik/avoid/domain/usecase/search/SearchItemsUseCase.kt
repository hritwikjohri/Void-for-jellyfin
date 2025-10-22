package com.hritwik.avoid.domain.usecase.search

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import com.hritwik.avoid.domain.model.auth.User
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.repository.LibraryRepository
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject


class SearchItemsUseCase @Inject constructor(
    private val libraryRepository: LibraryRepository
) : BaseUseCase<SearchItemsUseCase.Params, List<MediaItem>>(Dispatchers.IO) {

    data class Params(
        val userId: User,
        val accessToken: String,
        val searchTerm: String,
        val includeItemTypes: List<String> = listOf("Movie", "Series", "Episode"),
        val startIndex: Int = 0,
        val limit: Int = 50
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<MediaItem>> {
        return libraryRepository.searchItems(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            searchTerm = parameters.searchTerm,
            includeItemTypes = parameters.includeItemTypes.joinToString(","),
            startIndex = parameters.startIndex,
            limit = parameters.limit
        )
    }
}