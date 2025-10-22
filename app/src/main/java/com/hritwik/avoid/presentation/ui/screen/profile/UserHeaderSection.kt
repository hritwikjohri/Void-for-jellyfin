package com.hritwik.avoid.presentation.ui.screen.profile

import android.net.Uri
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun UserHeaderSection(
    userData: UserData,
    userId: String? = null,
    accessToken: String? = null,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val infiniteTransition = rememberInfiniteTransition(label = "background animation")
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient offset"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(calculateRoundedValue(16).sdp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(4).sdp)
    ) {
        val brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.primary
            ),
            start = Offset(offset * size.width.toFloat(), 0f),
            end = Offset(offset * size.width.toFloat() + size.width.toFloat(), size.height.toFloat())
        )

        val profileImageUrl = remember(userData.serverUrl, userId, accessToken) {
            val baseUrl = userData.serverUrl?.takeIf { it.isNotBlank() } ?: return@remember null
            val targetUserId = userId?.takeIf { it.isNotBlank() } ?: return@remember null
            val normalizedBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

            buildString {
                append(normalizedBase)
                append("UserImage?userId=")
                append(Uri.encode(targetUserId))
                append("&quality=")
                append(ApiConstants.DEFAULT_IMAGE_QUALITY)
                if (!accessToken.isNullOrBlank()) {
                    append("&api_key=")
                    append(Uri.encode(accessToken))
                }
            }
        }

        val context = LocalContext.current

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size = it }
                .background(brush)
                .padding(calculateRoundedValue(20).sdp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    
                    Box(
                        modifier = Modifier
                            .size(calculateRoundedValue(64).sdp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profileImageUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(profileImageUrl)
                                    .crossfade(true)
                                    .placeholder(R.drawable.void_icon)
                                    .error(R.drawable.void_icon)
                                    .build(),
                                contentDescription = "User Avatar",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "User Avatar",
                                modifier = Modifier.size(calculateRoundedValue(40).sdp),
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(calculateRoundedValue(16).sdp))

                    Column {
                        Text(
                            text = userData.name?: "User",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = userData.serverName?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}