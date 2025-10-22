package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.presentation.ui.components.common.NetworkImage
import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.LibraryType
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun LibraryGridCard(
    library: Library,
    serverUrl: String,
    modifier: Modifier = Modifier,
    onClick: (Library) -> Unit = {}
) {
    Card(
        modifier = modifier
            .aspectRatio(16f/9f)
            .clickable { onClick(library) },
        elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(8).sdp),
        shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (library.primaryImageTag != null) {
                val imageUrl = remember(serverUrl, library.id, library.primaryImageTag) {
                    createLibraryImageUrl(serverUrl, library.id, library.primaryImageTag)
                }
                NetworkImage(
                    data = imageUrl,
                    contentDescription = library.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getLibraryIcon(library.type),
                        contentDescription = null,
                        modifier = Modifier.size(calculateRoundedValue(48).sdp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .align(Alignment.BottomCenter)
                    .padding(calculateRoundedValue(12).sdp)
            ) {
                Text(
                    text = library.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getLibraryIcon(type: LibraryType): ImageVector {
    return when (type) {
        LibraryType.MOVIES -> Icons.Default.Movie
        LibraryType.TV_SHOWS -> Icons.Default.Tv
        LibraryType.MUSIC -> Icons.Default.MusicNote
        LibraryType.PHOTOS -> Icons.Default.Photo
        LibraryType.COLLECTIONS -> Icons.Default.Movie
        else -> Icons.Default.Movie
    }
}

private fun createLibraryImageUrl(
    serverUrl: String,
    itemId: String,
    imageTag: String
): String {
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$itemId/Images/Primary?tag=$imageTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.POSTER_MAX_WIDTH}"
}