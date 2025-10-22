package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem
import com.hritwik.avoid.presentation.ui.theme.WatchedStatus
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun MediaItemCard(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    serverUrl: String = "",
    badgeNumber: Int? = null,
    cardType: MediaCardType = MediaCardType.POSTER,
    showProgress: Boolean = true,
    showTitle: Boolean = true,
    customImageUrl: String? = null,
    isWatched: Boolean = false,
    onClick: (MediaItem) -> Unit = {}
) {
    val imageHelper = LocalImageHelper.current

    val aspectRatio = remember(cardType) {
        when (cardType) {
            MediaCardType.POSTER -> 2f / 3.15f
            MediaCardType.THUMBNAIL -> 16f / 9f
            MediaCardType.SQUARE -> 1f
        }
    }

    val defaultWidth = remember(cardType) {
        when (cardType) {
            MediaCardType.POSTER -> calculateRoundedValue(120)
            MediaCardType.THUMBNAIL -> calculateRoundedValue(240)
            MediaCardType.SQUARE -> calculateRoundedValue(120)
        }
    }.sdp

    Column(
        modifier = modifier.width(defaultWidth)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { onClick(mediaItem) }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).sdp),
            shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val imageUrl = remember(
                    serverUrl,
                    mediaItem.id,
                    mediaItem.primaryImageTag,
                    mediaItem.backdropImageTags,
                    cardType,
                    customImageUrl
                ) {
                    customImageUrl ?: if (cardType == MediaCardType.THUMBNAIL) {
                        imageHelper.createBackdropUrl(
                            serverUrl,
                            mediaItem.id,
                            mediaItem.backdropImageTags.firstOrNull()
                        ) ?: mediaItem.primaryImageTag?.let {
                            createMediaImageUrl(serverUrl, mediaItem.id, it)
                        }
                    } else {
                        mediaItem.primaryImageTag?.let {
                            createMediaImageUrl(serverUrl, mediaItem.id, it)
                        }
                    }
                }

                if (imageUrl != null) {
                    NetworkImage(
                        data = imageUrl,
                        contentDescription = mediaItem.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.FillBounds
                    )
                } else {
                    EmptyItem()
                }

                val watched by remember(isWatched, mediaItem.userData?.played) {
                    derivedStateOf { isWatched || mediaItem.userData?.played == true }
                }

                val showWatchedTag by remember(watched, mediaItem.type) {
                    derivedStateOf {
                        watched && mediaItem.type.equals(ApiConstants.ITEM_TYPE_EPISODE, ignoreCase = true)
                    }
                }

                if (showProgress && mediaItem.userData?.playbackPositionTicks != null &&
                    mediaItem.runTimeTicks != null && mediaItem.runTimeTicks > 0) {
                    val progress by remember(mediaItem.userData.playbackPositionTicks, mediaItem.runTimeTicks) {
                        derivedStateOf {
                            mediaItem.userData.playbackPositionTicks.toFloat() /
                                mediaItem.runTimeTicks.toFloat()
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(calculateRoundedValue(4).sdp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                badgeNumber?.let { number ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(calculateRoundedValue(4).sdp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(calculateRoundedValue(4).sdp)
                            )
                            .padding(horizontal = calculateRoundedValue(6).sdp, vertical = calculateRoundedValue(2).sdp)
                    ) {
                        Text(
                            text = number.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showWatchedTag) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(calculateRoundedValue(6).sdp)
                            .background(
                                color = WatchedStatus.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(calculateRoundedValue(6).sdp)
                            )
                            .padding(
                                horizontal = calculateRoundedValue(6).sdp,
                                vertical = calculateRoundedValue(2).sdp
                            )
                    ) {
                        Text(
                            text = "Watched",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (showTitle) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = calculateRoundedValue(8).sdp)
            ) {
                if (mediaItem.type == ApiConstants.ITEM_TYPE_EPISODE && mediaItem.seriesName != null) {
                    Text(
                        text = mediaItem.seriesName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val seasonNumber = mediaItem.parentIndexNumber
                    val episodeNumber = mediaItem.indexNumber
                    if (seasonNumber != null && episodeNumber != null) {
                        Text(
                            text = "S$seasonNumber . E$episodeNumber",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Text(
                        text = mediaItem.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun createMediaImageUrl(
    serverUrl: String,
    itemId: String,
    imageTag: String
): String {
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$itemId/Images/Primary?tag=$imageTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.THUMBNAIL_MAX_WIDTH}"
}
