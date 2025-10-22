package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun CollectionItemCard(
    collection: MediaItem,
    previewItems: List<MediaItem>,
    serverUrl: String,
    modifier: Modifier = Modifier,
    onClick: (MediaItem) -> Unit = {}
) {
    val imageHelper = LocalImageHelper.current
    val cardWidth = remember { calculateRoundedValue(120) }.sdp
    val cardShape = remember { RoundedCornerShape(calculateRoundedValue(12)) }
    val posterAspectRatio = remember { 2f / 3.15f }

    val fallbackImageUrl = remember(
        serverUrl,
        collection.id,
        collection.primaryImageTag,
        collection.backdropImageTags
    ) {
        imageHelper.createPosterUrl(
            serverUrl = serverUrl,
            itemId = collection.id,
            imageTag = imageHelper.getBestImageTag(
                collection.primaryImageTag,
                collection.backdropImageTags
            )
        )
    }

    val stackItems = remember(previewItems, collection) {
        val limited = previewItems.take(3)
        limited.ifEmpty { listOf(collection) }
    }

    Column(modifier = modifier.width(cardWidth)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(posterAspectRatio)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick(collection) }
                ),
            contentAlignment = Alignment.Center
        ) {
            stackItems.forEachIndexed { index, item ->
                val rotation = when (index) {
                    1 -> -10f
                    2 -> 10f
                    else -> 0f
                }
                val offsetX = when (index) {
                    1 -> (-calculateRoundedValue(12)).sdp
                    2 -> calculateRoundedValue(12).sdp
                    else -> 0.sdp
                }
                val elevation = when (index) {
                    0 -> calculateRoundedValue(6).sdp
                    else -> calculateRoundedValue(2).sdp
                }
                val zIndex = when (index) {
                    0 -> 3f
                    1 -> 2f
                    2 -> 1f
                    else -> 0f
                }

                val imageUrl = remember(
                    serverUrl,
                    item.id,
                    item.primaryImageTag,
                    item.backdropImageTags,
                    fallbackImageUrl
                ) {
                    imageHelper.createPosterUrl(
                        serverUrl = serverUrl,
                        itemId = item.id,
                        imageTag = imageHelper.getBestImageTag(
                            item.primaryImageTag,
                            item.backdropImageTags
                        )
                    ) ?: fallbackImageUrl
                }

                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxSize()
                        .offset(x = offsetX)
                        .rotate(rotation)
                        .zIndex(zIndex),
                    shape = cardShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = elevation)
                ) {
                    if (imageUrl != null) {
                        NetworkImage(
                            data = imageUrl,
                            contentDescription = item.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyItem()
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

        Text(
            text = collection.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )

        collection.childCount?.takeIf { it > 0 }?.let { count ->
            Spacer(modifier = Modifier.height(calculateRoundedValue(4).sdp))
            Text(
                text = "$count titles",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
