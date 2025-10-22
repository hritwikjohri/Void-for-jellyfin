package com.hritwik.avoid.presentation.viewmodel.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.usecase.library.GetCollectionItemsUseCase
import com.hritwik.avoid.domain.usecase.library.GetCollectionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionsViewModel @Inject constructor(
    private val getCollectionsUseCase: GetCollectionsUseCase,
    private val getCollectionItemsUseCase: GetCollectionItemsUseCase
) : ViewModel() {

    private val itemsPerPage = 60
    private val previewLimit = 3

    private val _collectionPreviews = MutableStateFlow<Map<String, List<MediaItem>>>(emptyMap())
    val collectionPreviews: StateFlow<Map<String, List<MediaItem>>> = _collectionPreviews.asStateFlow()

    private val previewRequests = mutableSetOf<String>()

    fun collectionsPager(
        userId: String,
        accessToken: String,
        tags: List<String>?
    ): Flow<PagingData<MediaItem>> =
        Pager(PagingConfig(pageSize = itemsPerPage)) {
            CollectionsPagingSource(
                getCollectionsUseCase = getCollectionsUseCase,
                userId = userId,
                accessToken = accessToken,
                tags = tags
            )
        }.flow.cachedIn(viewModelScope)

    fun ensureCollectionPreview(
        userId: String,
        accessToken: String,
        collectionId: String
    ) {
        if (_collectionPreviews.value.containsKey(collectionId)) return
        if (!previewRequests.add(collectionId)) return

        viewModelScope.launch {
            try {
                when (
                    val result = getCollectionItemsUseCase(
                        GetCollectionItemsUseCase.Params(
                            userId = userId,
                            accessToken = accessToken,
                            collectionId = collectionId,
                            limit = previewLimit
                        )
                    )
                ) {
                    is NetworkResult.Success -> {
                        val items = result.data.take(previewLimit)
                        _collectionPreviews.update { it + (collectionId to items) }
                    }

                    is NetworkResult.Error -> {
                        _collectionPreviews.update { it + (collectionId to emptyList()) }
                    }

                    is NetworkResult.Loading -> Unit
                }
            } finally {
                previewRequests.remove(collectionId)
            }
        }
    }
}
