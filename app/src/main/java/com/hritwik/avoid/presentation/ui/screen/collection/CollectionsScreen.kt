package com.hritwik.avoid.presentation.ui.screen.collection

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.ContentGrid
import com.hritwik.avoid.presentation.ui.components.common.ErrorDisplay
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.common.states.EmptyState
import com.hritwik.avoid.presentation.ui.components.common.states.LoadingState
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.library.CollectionsViewModel

@Composable
fun CollectionsScreen(
    onBackClick: () -> Unit,
    onCollectionClick: (MediaItem) -> Unit,
    authViewModel: AuthServerViewModel,
    collectionsViewModel: CollectionsViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val collectionPreviews by collectionsViewModel.collectionPreviews.collectAsStateWithLifecycle()
    val gridState = rememberSaveable(saver = LazyGridState.Saver) { LazyGridState() }

    val pagingItems = authState.authSession?.let { session ->
        val pager = remember(session.userId.id, session.accessToken) {
            collectionsViewModel.collectionsPager(
                userId = session.userId.id,
                accessToken = session.accessToken,
                tags = null
            )
        }
        pager.collectAsLazyPagingItems()
    }


    AnimatedAmbientBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenHeader(
                title = "Collections",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when {
                    pagingItems == null || pagingItems.loadState.refresh is LoadState.Loading -> {
                        LoadingState()
                    }

                    pagingItems.loadState.refresh is LoadState.Error -> {
                        val message = (pagingItems.loadState.refresh as LoadState.Error).error.message ?: ""
                        ErrorDisplay(
                            error = message,
                            onDismiss = { pagingItems.retry() }
                        )
                    }

                    pagingItems.itemCount == 0 -> {
                        EmptyState(
                            title = "No collections yet",
                            description = "Collections will appear here once they are available."
                        )
                    }

                    else -> {
                        ContentGrid(
                            items = pagingItems,
                            gridState = gridState,
                            serverUrl = authState.authSession?.server?.url ?: "",
                            onMediaItemClick = onCollectionClick,
                            showAlphaScroller = false,
                            collectionPreviewProvider = { mediaItem ->
                                collectionPreviews[mediaItem.id].orEmpty()
                            },
                            onCollectionVisible = { mediaItem ->
                                authState.authSession?.let { session ->
                                    collectionsViewModel.ensureCollectionPreview(
                                        userId = session.userId.id,
                                        accessToken = session.accessToken,
                                        collectionId = mediaItem.id
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (pagingItems != null && pagingItems.loadState.append is LoadState.Error) {
                val message = (pagingItems.loadState.append as LoadState.Error).error.message ?: ""
                ErrorDisplay(
                    error = message,
                    onDismiss = { pagingItems.retry() }
                )
            }
        }
    }
}
