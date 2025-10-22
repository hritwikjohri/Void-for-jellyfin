package com.hritwik.avoid.presentation.viewmodel.library

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.library.GetCollectionsUseCase

class CollectionsPagingSource(
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val userId: String,
    private val accessToken: String,
    private val tags: List<String>?
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val startIndex = params.key ?: 0
        return when (
            val result = getCollectionsUseCase(
                GetCollectionsUseCase.Params(
                    userId = userId,
                    accessToken = accessToken,
                    startIndex = startIndex,
                    limit = params.loadSize,
                    tags = tags
                )
            )
        ) {
            is NetworkResult.Success -> {
                val data = result.data
                val nextKey = if (data.isEmpty()) null else startIndex + data.size
                LoadResult.Page(
                    data = data,
                    prevKey = if (startIndex == 0) null else maxOf(0, startIndex - params.loadSize),
                    nextKey = nextKey
                )
            }

            is NetworkResult.Error -> LoadResult.Error(Throwable(result.message))
            is NetworkResult.Loading -> LoadResult.Page(emptyList(), prevKey = null, nextKey = null)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition) ?: return null

        anchorPage.nextKey?.let { nextKey ->
            val calculatedStart = nextKey - anchorPage.data.size
            if (calculatedStart >= 0) {
                return calculatedStart
            }
        }

        anchorPage.prevKey?.let { prevKey ->
            return maxOf(0, prevKey + state.config.pageSize)
        }

        return null
    }
}
