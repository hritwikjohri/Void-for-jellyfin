package com.hritwik.avoid.presentation.viewmodel.library

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.usecase.library.GetLibraryItemsUseCase

class LibraryItemsPagingSource(
    private val getLibraryItemsUseCase: GetLibraryItemsUseCase,
    private val userId: String,
    private val libraryId: String,
    private val accessToken: String,
    private val sortBy: List<String>,
    private val sortOrder: LibrarySortDirection,
    private val genre: String?,
    private val forceRefreshOnFirstLoad: Boolean
) : PagingSource<Int, MediaItem>() {

    private var hasForcedRefresh = false

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        val startIndex = params.key ?: 0
        val shouldForceRefresh = forceRefreshOnFirstLoad && !hasForcedRefresh && startIndex == 0

        if (shouldForceRefresh) {
            hasForcedRefresh = true
        }

        return when (val result = getLibraryItemsUseCase(
                GetLibraryItemsUseCase.Params(
                    userId = userId,
                    libraryId = libraryId,
                    accessToken = accessToken,
                    startIndex = startIndex,
                    limit = params.loadSize,
                    forceRefresh = shouldForceRefresh,
                    sortBy = sortBy,
                    sortOrder = sortOrder,
                    genre = genre
                )
        )) {
            is NetworkResult.Success -> {
                val nextKey = if (result.data.isEmpty()) null else startIndex + result.data.size
                LoadResult.Page(
                    data = result.data,
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
