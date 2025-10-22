package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.extensions.formatRuntime
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.presentation.ui.theme.WatchedStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay


@Composable
fun EpisodeCard(
    episode: MediaItem,
    episodeNumber: Int,
    serverUrl: String,
    modifier: Modifier = Modifier,
    isWatched: Boolean = false,
    highlighted: Boolean = false,
    onHighlightFinished: () -> Unit = {},
    onClick: (MediaItem) -> Unit = {}
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var highlightActive by remember { mutableStateOf(false) }
    val defaultBorderColor = Color.White.copy(alpha = 0.2f)
    val defaultBorderWidth = remember { calculateRoundedValue(1) }.sdp
    val highlightedBorderWidth = remember { calculateRoundedValue(2) }.sdp
    val borderColor by animateColorAsState(
        targetValue = if (highlightActive) MaterialTheme.colorScheme.primary else defaultBorderColor,
        animationSpec = tween(durationMillis = 300),
        label = "episodeCardBorderColor"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (highlightActive) highlightedBorderWidth else defaultBorderWidth,
        animationSpec = tween(durationMillis = 300),
        label = "episodeCardBorderWidth"
    )

    LaunchedEffect(highlighted) {
        if (highlighted) {
            highlightActive = true
            try {
                bringIntoViewRequester.bringIntoView()
            } catch (_: CancellationException) {
                
            }
            delay(1_000)
            highlightActive = false
            onHighlightFinished()
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(calculateRoundedValue(8).sdp)
            .bringIntoViewRequester(bringIntoViewRequester)
            .clickable { onClick(episode) },
        elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(0).sdp),
        shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        border = BorderStroke(
            width = borderWidth,
            color = borderColor
        )
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.08f)
                            ),
                            radius = 600f
                        ),
                        shape = RoundedCornerShape(calculateRoundedValue(16).sdp)
                    )
                    .blur(radius = calculateRoundedValue(10).sdp) 
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .matchParentSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.08f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.05f)
                            )
                        ),
                        shape = RoundedCornerShape(calculateRoundedValue(16).sdp)
                    )
            )

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(calculateRoundedValue(12).sdp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                Box(
                    modifier = Modifier
                        .width(calculateRoundedValue(120).sdp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(calculateRoundedValue(8).sdp))
                ) {
                    val thumbnailUrl = remember(serverUrl, episode.id, episode.primaryImageTag, episode.backdropImageTags) {
                        createEpisodeThumbnailUrl(
                            serverUrl,
                            episode.id,
                            episode.backdropImageTags.firstOrNull() ?: episode.primaryImageTag
                        )
                    }

                    if (thumbnailUrl != null) {
                        NetworkImage(
                            data = thumbnailUrl,
                            contentDescription = episode.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(calculateRoundedValue(24).sdp),
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    
                    episode.userData?.let { userData ->
                        if (userData.playbackPositionTicks > 0 && episode.runTimeTicks != null && episode.runTimeTicks > 0) {
                            val progress = userData.playbackPositionTicks.toFloat() / episode.runTimeTicks.toFloat()
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(calculateRoundedValue(3).sdp)
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

                
                val watched = isWatched || episode.userData?.played == true

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "$episodeNumber. ${episode.name}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )

                    if (!episode.overview.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(calculateRoundedValue(4).sdp))
                        Text(
                            text = episode.overview,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        episode.runTimeTicks?.let { runtime ->
                            Text(
                                text = runtime.formatRuntime(),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        if (watched) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Watched",
                                style = MaterialTheme.typography.bodySmall,
                                color = WatchedStatus,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}


private fun createEpisodeThumbnailUrl(
    serverUrl: String,
    itemId: String,
    imageTag: String?
): String? {
    if (imageTag == null) return null
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$itemId/Images/Primary?tag=$imageTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.THUMBNAIL_MAX_WIDTH}"
}