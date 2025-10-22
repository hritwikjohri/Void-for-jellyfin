package com.hritwik.avoid.presentation.ui.components.media

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun ActiveDownloadCard(
    mediaItem: MediaItem,
    progress: Float,
    modifier: Modifier = Modifier,
    serverUrl: String = "",
    cardType: MediaCardType = MediaCardType.POSTER,
    showTitle: Boolean = true,
    onClick: (MediaItem) -> Unit = {},
) {
    Column(modifier = modifier) {
        MediaItemCard(
            mediaItem = mediaItem,
            serverUrl = serverUrl,
            cardType = cardType,
            showProgress = false,
            showTitle = showTitle,
            onClick = onClick,
        )
        LinearProgressIndicator(
            progress = { progress / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = calculateRoundedValue(4).sdp),
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = String.format("%.1f%%", progress),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = calculateRoundedValue(4).sdp)
        )
    }
}
