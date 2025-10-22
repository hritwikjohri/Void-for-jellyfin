package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    mediaItem: MediaItem,
    serverUrl: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
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
                    Text(
                        text = mediaItem.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(calculateRoundedValue(16).sdp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Text(
                        text = mediaItem.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}