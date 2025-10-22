package com.hritwik.avoid.domain.usecase.jellyseer

import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerPagedResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.repository.JellyseerRepository
import com.hritwik.avoid.domain.usecase.common.BaseUseCase
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

class SearchJellyseerMediaUseCase @Inject constructor(
    private val repository: JellyseerRepository
) : BaseUseCase<SearchJellyseerMediaUseCase.Params, JellyseerPagedResult<JellyseerSearchResult>>(Dispatchers.IO) {

    data class Params(
        val query: String,
        val page: Int = 1,
        val language: String? = null
    )

    override suspend fun execute(parameters: Params): NetworkResult<JellyseerPagedResult<JellyseerSearchResult>> {
        return repository.search(
            query = parameters.query,
            page = parameters.page,
            language = parameters.language
        )
    }
}
