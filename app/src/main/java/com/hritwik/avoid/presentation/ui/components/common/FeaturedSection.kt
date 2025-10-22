package com.hritwik.avoid.presentation.ui.components.common

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.utils.extensions.getPosterUrl
import com.hritwik.avoid.utils.helpers.LocalImageHelper
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

@Composable
fun FeaturedPagerSection(
    mediaItems: List<MediaItem>,
    serverUrl: String,
    onSearchClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onPlayClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    autoScroll: Boolean = true,
    autoScrollDelayMs: Long = 10000L
) {
    if (mediaItems.isEmpty()) return

    val imageHelper = LocalImageHelper.current

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { mediaItems.size }
    )

    LaunchedEffect(autoScroll, mediaItems.size) {
        if (autoScroll && mediaItems.size > 1) {
            while (true) {
                delay(autoScrollDelayMs)
                val nextPage = (pagerState.currentPage + 1) % mediaItems.size
                pagerState.animateScrollToPage(
                    page = nextPage,
                    animationSpec = tween(durationMillis = 1000)
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(calculateRoundedValue(600).sdp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val mediaItem = mediaItems[page]

            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                val backdropUrl = remember(serverUrl, mediaItem.id, mediaItem.backdropImageTags) {
                    imageHelper.createBackdropUrl(
                        serverUrl,
                        mediaItem.id,
                        mediaItem.backdropImageTags.firstOrNull()
                    )
                }
                NetworkImage(
                    data = backdropUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = calculateRoundedValue(1).sdp)
                        .graphicsLayer {
                            alpha = lerp(
                                start = 0.3f,
                                stop = 1f,
                                fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                            )
                        },
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Black
                                ),
                                radius = 1200f
                            )
                        )
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black
                                )
                            )
                        )
                )

                FeaturedContentCard(
                    mediaItem = mediaItem,
                    serverUrl = serverUrl,
                    onPlayClick = onPlayClick,
                    onMoreClick = { onMediaClick(mediaItem) },
                    pageOffset = pageOffset,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        FloatingActionButton(
            onClick = onSearchClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(calculateRoundedValue(16).sdp)
                .size(calculateRoundedValue(56).sdp)
                .zIndex(10f),
            containerColor = Color.Transparent,
            contentColor = PrimaryText,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = calculateRoundedValue(0).sdp,
                pressedElevation = calculateRoundedValue(0).sdp
            )
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                modifier = Modifier.size(calculateRoundedValue(24).sdp)
            )
        }

        if (mediaItems.size > 1) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = calculateRoundedValue(32).sdp)
                    .zIndex(10f),
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
            ) {
                repeat(mediaItems.size) { index ->
                    val isSelected = index == pagerState.currentPage
                    val animatedWidth by animateFloatAsState(
                        targetValue = if (isSelected) 8f else 4f,
                        animationSpec = tween(500), label = ""
                    )

                    Box(
                        modifier = Modifier
                            .width(animatedWidth.dp)
                            .height(calculateRoundedValue(4).sdp)
                            .background(
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(calculateRoundedValue(2).sdp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedContentCard(
    mediaItem: MediaItem,
    serverUrl: String,
    onPlayClick: (MediaItem) -> Unit,
    onMoreClick: () -> Unit,
    pageOffset: Float,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = lerp(0.85f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)),
        animationSpec = tween(300), label = ""
    )

    val alpha by animateFloatAsState(
        targetValue = lerp(0.3f, 1f, 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)),
        animationSpec = tween(300), label = ""
    )

    Column(
        modifier = modifier
            .scale(scale)
            .alpha(alpha)
            .padding(calculateRoundedValue(24).sdp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .width(calculateRoundedValue(200).sdp)
                .height(calculateRoundedValue(280).sdp)
                .graphicsLayer {
                    rotationY = pageOffset * 15f
                    shadowElevation = 20f
                },
            shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
            elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(12).sdp)
        ) {
            val posterUrl = remember(serverUrl, mediaItem.id, mediaItem.primaryImageTag, mediaItem.backdropImageTags) {
                mediaItem.getPosterUrl(serverUrl)
            }
            NetworkImage(
                data = posterUrl,
                contentDescription = mediaItem.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tags = remember(mediaItem) {
                listOf(
                    mediaItem.year?.toString() ?: "",
                    mediaItem.genres.firstOrNull() ?: "Movie",
                ).filter { it.isNotBlank() }
            }
            tags.forEach { tag ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = calculateRoundedValue(8).sdp, vertical = calculateRoundedValue(4).sdp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(calculateRoundedValue(32).sdp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { onPlayClick(mediaItem) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(calculateRoundedValue(32).sdp),
                modifier = Modifier.height(calculateRoundedValue(56).sdp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    modifier = Modifier.size(calculateRoundedValue(24).sdp)
                )
                Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                Text(
                    text = "Watch Now",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            FilledTonalButton(
                onClick = onMoreClick,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.15f),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(calculateRoundedValue(32).sdp),
                modifier = Modifier.height(calculateRoundedValue(56).sdp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "More info",
                    modifier = Modifier.size(calculateRoundedValue(20).sdp)
                )
            }
        }
    }
}