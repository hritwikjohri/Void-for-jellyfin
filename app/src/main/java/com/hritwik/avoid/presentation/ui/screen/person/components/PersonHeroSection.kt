package com.hritwik.avoid.presentation.ui.screen.person.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.common.EmptyItem
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun PersonHeroSection(
    personDetail: MediaItem,
    serverUrl: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Int = 350
) {
    val imageUrl = personDetail.primaryImageTag?.let { tag ->
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        "${baseUrl}Items/${personDetail.id}/Images/Primary?tag=$tag&quality=90&maxWidth=800"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
    ) {
        // Background Image (blurred)
        if (imageUrl != null) {
            AsyncImage(
                model = imageUrl,
                contentDescription = personDetail.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient Overlay
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

        // Back Button
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(calculateRoundedValue(4).sdp, calculateRoundedValue(8).sdp)
                .statusBarsPadding()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(calculateRoundedValue(24).sdp)
            )
        }

        // Person Info
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(calculateRoundedValue(16).sdp),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Person Image (circular)
            Box(
                modifier = Modifier
                    .size(calculateRoundedValue(120).sdp)
                    .clip(CircleShape)
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = personDetail.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    EmptyItem(
                        image = R.drawable.person,
                    )
                }
            }

            // Person Name
            Text(
                text = personDetail.name,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
