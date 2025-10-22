package com.hritwik.avoid.presentation.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.search.GetItemsByCategoryUseCase
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.presentation.ui.state.LibraryItemsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val getItemsByCategoryUseCase: GetItemsByCategoryUseCase
) : ViewModel() {

    private val _categoryState = MutableStateFlow(LibraryItemsState())
    val categoryState: StateFlow<LibraryItemsState> = _categoryState.asStateFlow()

    private val itemsPerPage = 50
    private val itemPool = ArrayDeque<MediaItem>()
    private val maxPoolSize = itemsPerPage * 3

    fun loadCategoryItems(
        userId: String,
        accessToken: String,
        filters: Map<String, String>,
        refresh: Boolean = false
    ) {
        viewModelScope.launch {
            if (refresh) {
                _categoryState.value = LibraryItemsState(isLoading = true)
                itemPool.clear()
            } else {
                _categoryState.value = _categoryState.value.copy(isLoading = true, error = null)
            }

            val page = if (refresh) 0 else _categoryState.value.currentPage
            val startIndex = page * itemsPerPage

            try {
                when (val result = getItemsByCategoryUseCase(
                    GetItemsByCategoryUseCase.Params(
                        userId = userId,
                        accessToken = accessToken,
                        filters = filters,
                        startIndex = startIndex,
                        limit = itemsPerPage
                    )
                )) {
                    is NetworkResult.Success -> {
                        itemPool.addAll(result.data)
                        while (itemPool.size > maxPoolSize) {
                            itemPool.removeFirst()
                        }
                        val hasMore = result.data.size == itemsPerPage
                        _categoryState.value = _categoryState.value.copy(
                            isLoading = false,
                            items = itemPool.toList(),
                            hasMorePages = hasMore,
                            currentPage = page + 1,
                            error = null
                        )
                    }
                    is NetworkResult.Error -> {
                        _categoryState.value = _categoryState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                    else -> {
                        _categoryState.value = _categoryState.value.copy(isLoading = false)
                    }
                }
            } catch (e: Exception) {
                _categoryState.value = _categoryState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun dropConsumedItems(count: Int) {
        repeat(count) { if (itemPool.isNotEmpty()) itemPool.removeFirst() }
        _categoryState.value = _categoryState.value.copy(items = itemPool.toList())
    }

    fun clearError() {
        _categoryState.value = _categoryState.value.copy(error = null)
    }

    fun categoryItemsPager(
        userId: String,
        accessToken: String,
        filters: Map<String, String>,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection
    ): Flow<PagingData<MediaItem>> =
        Pager(PagingConfig(pageSize = itemsPerPage)) {
            CategoryPagingSource(
                getItemsByCategoryUseCase = getItemsByCategoryUseCase,
                userId = userId,
                accessToken = accessToken,
                filters = filters,
                sortBy = sortBy,
                sortOrder = sortOrder
            )
        }.flow.cachedIn(viewModelScope)
}
