package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.state.HeroStyle
import com.hritwik.avoid.utils.extensions.getPosterUrl
import com.hritwik.avoid.utils.helpers.ImageHelper

@Composable
fun MediaHeroSection(
    modifier: Modifier = Modifier,
    mediaItem: MediaItem,
    serverUrl: String,
    onBackClick: () -> Unit,
    onSeasonClick: (String, String) -> Unit = { _, _ -> },
    onEpisodeTitleClick: () -> Unit = {},
    onHomeClick: () -> Unit = {},
    onSeasonSwitcherClick: (() -> Unit)? = null,
    isThemeSongPlaying: Boolean = false,
    showThemeSongToggle: Boolean = false,
    onThemeSongToggle: () -> Unit = {},
    heroStyle: HeroStyle = HeroStyle.MOVIE_SERIES,
    height: Int = 350
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {
        when (heroStyle) {
            HeroStyle.SEASON -> {
                AsyncImage(
                    model = mediaItem.getPosterUrl(serverUrl),
                    contentDescription = mediaItem.name,
                    modifier = Modifier.fillMaxSize().blur(calculateRoundedValue(12).sdp),
                    contentScale = ContentScale.Crop
                )
            }
            HeroStyle.EPISODE -> {
                AsyncImage(
                    model = mediaItem.getPosterUrl(serverUrl),
                    contentDescription = mediaItem.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                AsyncImage(
                    model = ImageHelper().createBackdropUrl(serverUrl, mediaItem.id, mediaItem.backdropImageTags.firstOrNull()),
                    contentDescription = mediaItem.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.9f)
                        ),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(calculateRoundedValue(4).sdp,calculateRoundedValue(8).sdp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(calculateRoundedValue(24).sdp)
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(calculateRoundedValue(4).sdp, calculateRoundedValue(8).sdp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(2).sdp)
        ) {
            if (showThemeSongToggle) {
                val infiniteTransition = rememberInfiniteTransition(label = "musicRotation")
                val animatedRotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 2000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "rotation"
                )
                IconButton(onClick = onThemeSongToggle) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = if (isThemeSongPlaying) "Stop theme music" else "Play theme music",
                        tint = if (isThemeSongPlaying) Color.White else Color.White.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(calculateRoundedValue(24).sdp)
                            .graphicsLayer {
                                rotationZ = if (isThemeSongPlaying) animatedRotation else 0f
                            }
                    )
                }
            }
            IconButton(onClick = onHomeClick) {
                Icon(
                    imageVector = Icons.Filled.Home,
                    contentDescription = "Go to home",
                    tint = Color.White,
                    modifier = Modifier.size(calculateRoundedValue(24).sdp)
                )
            }
        }

        when (heroStyle) {
            HeroStyle.SEASON -> {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(calculateRoundedValue(12).sdp),
                    horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        model = mediaItem.getPosterUrl(serverUrl),
                        contentDescription = mediaItem.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth(0.33f),
                    )
                    val titleModifier = if (onSeasonSwitcherClick != null) {
                        Modifier
                            .weight(1f)
                            .clickable { onSeasonSwitcherClick() }
                    } else {
                        Modifier.weight(1f)
                    }
                    Row(
                        modifier = titleModifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
                    ) {
                        Text(
                            text = mediaItem.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (onSeasonSwitcherClick != null) {
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = "Select season",
                                tint = Color.White,
                                modifier = Modifier.size(calculateRoundedValue(20).sdp)
                            )
                        }
                    }
                }
            }

            HeroStyle.MOVIE_SERIES -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(calculateRoundedValue(16).sdp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    mediaItem.getLogoUrl(serverUrl)?.let { logoUrl ->
                        AsyncImage(
                            model = logoUrl,
                            contentDescription = mediaItem.name,
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .heightIn(max = (height * 0.3f).dp),
                            contentScale = ContentScale.Fit,
                            alignment = Alignment.BottomCenter
                        )
                    } ?: run {
                        Text(
                            text = mediaItem.name,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            HeroStyle.EPISODE -> {
                val episodeStyle = MaterialTheme.typography.headlineMedium
                val seasonLabel = mediaItem.seasonName?.takeIf { it.isNotBlank() }
                    ?: mediaItem.parentIndexNumber?.let { "Season $it" }
                val seasonId = mediaItem.seasonId
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(calculateRoundedValue(16).sdp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column(
                        modifier = Modifier.clickable { onEpisodeTitleClick() },
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp)
                    ) {
                        if (seasonId != null && seasonLabel != null) {
                            Text(
                                text = seasonLabel,
                                style = episodeStyle.copy(fontSize = episodeStyle.fontSize * 0.5f),
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { onSeasonClick(seasonId, seasonLabel) }
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(6).sdp),
                            modifier = Modifier.clickable { onEpisodeTitleClick() }
                        ) {
                            Text(
                                text = mediaItem.name,
                                style = episodeStyle,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Icon(
                                imageVector = Icons.Filled.ExpandMore,
                                contentDescription = "Select episode",
                                tint = Color.White,
                                modifier = Modifier.size(calculateRoundedValue(18).sdp)
                            )
                        }
                    }
                }
            }
        }
    }
}
