package com.hritwik.avoid.presentation.ui.components.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.layout.SectionHeader
import com.hritwik.avoid.presentation.ui.components.media.MediaCardType
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

/**
 * Reusable home screen section component with proper Compose keys for optimal recomposition.
 * Extracted from monolithic Home.kt to improve performance and maintainability.
 */
@Composable
fun HomeSection(
    title: String,
    keyPrefix: String,
    items: List<MediaItem>,
    serverUrl: String,
    cardType: MediaCardType = MediaCardType.POSTER,
    showProgress: Boolean = false,
    showTitle: Boolean = true,
    playedItems: Set<String> = emptySet(),
    customImageProvider: ((MediaItem) -> String?)? = null,
    shouldLoadImages: Boolean = true,
    onMediaItemClick: (MediaItem) -> Unit
) {
    if (items.isEmpty()) return

    SectionHeader(title = title) {
        LazyRow(
            modifier = Modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
            contentPadding = PaddingValues(horizontal = calculateRoundedValue(10).sdp)
        ) {
            itemsIndexed(
                items = items,
                key = { _, item -> "${keyPrefix}_${item.id}" }
            ) { _, mediaItem ->
                val customImageUrl = customImageProvider?.let { provider ->
                    provider(mediaItem)
                }

                MediaItemCard(
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    cardType = cardType,
                    showProgress = showProgress,
                    showTitle = showTitle,
                    customImageUrl = customImageUrl,
                    isWatched = playedItems.contains(mediaItem.id),
                    shouldLoadImages = shouldLoadImages,
                    onClick = onMediaItemClick
                )
            }
        }
    }
}
