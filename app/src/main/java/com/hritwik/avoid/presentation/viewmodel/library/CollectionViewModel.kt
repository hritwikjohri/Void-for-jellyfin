package com.hritwik.avoid.presentation.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hritwik.avoid.domain.model.library.LibrarySortDirection
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.library.GetCollectionItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val getCollectionItemsUseCase: GetCollectionItemsUseCase
) : ViewModel() {

    private val itemsPerPage = 50

    fun collectionItemsPager(
        userId: String,
        accessToken: String,
        collectionId: String,
        sortBy: List<String>,
        sortOrder: LibrarySortDirection
    ): Flow<PagingData<MediaItem>> =
        Pager(PagingConfig(pageSize = itemsPerPage)) {
            CollectionItemsPagingSource(
                getCollectionItemsUseCase = getCollectionItemsUseCase,
                userId = userId,
                collectionId = collectionId,
                accessToken = accessToken,
                sortBy = sortBy,
                sortOrder = sortOrder
            )
        }.flow.cachedIn(viewModelScope)
}
