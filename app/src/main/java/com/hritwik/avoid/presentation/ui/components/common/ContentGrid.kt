package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.paging.compose.LazyPagingItems
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.media.CollectionItemCard
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun ContentGrid(
    items: LazyPagingItems<MediaItem>,
    gridState: LazyGridState,
    serverUrl: String,
    onMediaItemClick: (MediaItem) -> Unit,
    showAlphaScroller: Boolean = true,
    collectionPreviewProvider: (MediaItem) -> List<MediaItem> = { emptyList() },
    onCollectionVisible: (MediaItem) -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
        ) {
            items(items.itemCount) { index ->
                items[index]?.let { mediaItem ->
                    if (mediaItem.type.equals("BoxSet", ignoreCase = true)) {
                        LaunchedEffect(mediaItem.id) {
                            onCollectionVisible(mediaItem)
                        }
                        CollectionItemCard(
                            collection = mediaItem,
                            previewItems = collectionPreviewProvider(mediaItem),
                            serverUrl = serverUrl,
                            modifier = Modifier.padding(8.sdp),
                            onClick = { onMediaItemClick(mediaItem) }
                        )
                    } else {
                        MediaItemCard(
                            mediaItem = mediaItem,
                            showProgress = false,
                            serverUrl = serverUrl,
                            onClick = { onMediaItemClick(mediaItem) }
                        )
                    }
                }
            }
        }

        if (showAlphaScroller) {
            var overlayLetter by remember { mutableStateOf<Char?>(null) }

            AnimatedVisibility(
                visible = overlayLetter != null,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(calculateRoundedValue(24).sdp),
                    tonalElevation = calculateRoundedValue(6).sdp
                ) {
                    Box(
                        modifier = Modifier
                            .padding(
                                horizontal = calculateRoundedValue(28).sdp,
                                vertical = calculateRoundedValue(16).sdp
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = overlayLetter?.toString() ?: "",
                            style = MaterialTheme.typography.displayMedium
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                AlphaScroller(
                    items = items.itemSnapshotList.items,
                    gridState = gridState,
                    modifier = Modifier
                        .align(Alignment.Center),
                    onActiveLetterChange = { overlayLetter = it }
                )
            }
        }
    }
}