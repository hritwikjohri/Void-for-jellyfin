package com.hritwik.avoid.domain.usecase.search

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.repository.SearchRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class GetSearchSuggestionsUseCase @Inject constructor(
    private val searchRepository: SearchRepository
) : BaseUseCase<GetSearchSuggestionsUseCase.Params, List<String>>(Dispatchers.IO) {

    data class Params(
        val userId: String,
        val accessToken: String,
        val query: String,
        val limit: Int = 10
    )

    override suspend fun execute(parameters: Params): NetworkResult<List<String>> {
        return searchRepository.getSearchSuggestions(
            userId = parameters.userId,
            accessToken = parameters.accessToken,
            query = parameters.query,
            limit = parameters.limit
        )
    }
}