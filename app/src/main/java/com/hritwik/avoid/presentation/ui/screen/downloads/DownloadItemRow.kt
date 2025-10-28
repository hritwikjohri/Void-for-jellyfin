package com.hritwik.avoid.presentation.ui.screen.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.data.download.DownloadService.DownloadInfo
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.DownloadDisplayFormatter
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DownloadItemRow(
    downloadInfo: DownloadInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    isActive: Boolean = false,
    selectionEnabled: Boolean = false,
    isSelected: Boolean = false,
    onSelectionChange: (Boolean) -> Unit = {}
) {
    val imageHelper = LocalImageHelper.current
    val mediaItem = downloadInfo.mediaItem
    val isEpisode = mediaItem.type == ApiConstants.ITEM_TYPE_EPISODE
    val aspectRatio = if (isEpisode) 16f / 9f else 2f / 3f
    val context = LocalContext.current
    val progressFraction = DownloadDisplayFormatter.progressFraction(downloadInfo)
    val showIndeterminateProgress = DownloadDisplayFormatter.isProgressIndeterminate(downloadInfo)

    val imageUrl = remember(downloadInfo.serverUrl, mediaItem.id, mediaItem.primaryImageTag, mediaItem.backdropImageTags) {
        if (isEpisode) {
            imageHelper.createBackdropUrl(
                downloadInfo.serverUrl,
                mediaItem.id,
                mediaItem.backdropImageTags.firstOrNull()
            ) ?: imageHelper.createPosterUrl(downloadInfo.serverUrl, mediaItem.id, mediaItem.primaryImageTag)
        } else {
            imageHelper.createPosterUrl(downloadInfo.serverUrl, mediaItem.id, mediaItem.primaryImageTag)
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = calculateRoundedValue(16).sdp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            shape = RoundedCornerShape(calculateRoundedValue(8).sdp),
            elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).sdp),
            modifier = Modifier
                .width(if (isEpisode) calculateRoundedValue(160).sdp else calculateRoundedValue(100).sdp)
                .aspectRatio(aspectRatio)
        ) {
            Box {
                if (imageUrl != null) {
                    NetworkImage(
                        data = imageUrl,
                        contentDescription = mediaItem.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    EmptyItem()
                }

                if (isActive && (progressFraction != null || showIndeterminateProgress)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (progressFraction != null) {
                            CircularProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier.size(32.dp),
                                color = ProgressIndicatorDefaults.circularColor,
                                strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
                                trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = ProgressIndicatorDefaults.circularColor,
                                strokeWidth = ProgressIndicatorDefaults.CircularStrokeWidth,
                                trackColor = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
                                strokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mediaItem.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            val metadataItems = buildList {
                mediaItem.type.takeIf { it.isNotBlank() }?.let { add(it) }
                mediaItem.year?.toString()?.let { add(it) }
                if (isEpisode) {
                    mediaItem.runTimeTicks?.let { ticks ->
                        val minutes = (ticks / 600_000_000).toInt()
                        add("${minutes}min")
                    }
                }
            }

            if (metadataItems.isNotEmpty()) {
                Text(
                    text = metadataItems.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isEpisode && mediaItem.seriesName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(mediaItem.seriesName)
                        mediaItem.parentIndexNumber?.let { season ->
                            mediaItem.indexNumber?.let { episode ->
                                append(" • S${season} • E${episode}")
                            }
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isActive) {
                Spacer(modifier = Modifier.height(4.dp))
                val statusText = DownloadDisplayFormatter.formatStatus(context, downloadInfo)
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }

        if (selectionEnabled) {
            Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange
            )
        }
    }
}