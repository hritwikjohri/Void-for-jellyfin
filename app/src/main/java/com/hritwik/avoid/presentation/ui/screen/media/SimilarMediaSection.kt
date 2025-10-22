package com.hritwik.avoid.presentation.ui.screen.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.components.media.MediaItemCard
import com.hritwik.avoid.presentation.ui.theme.PrimaryText

@Composable
fun SimilarMediaSection(
    title: String,
    items: List<MediaItem>,
    serverUrl: String,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    showProgress: Boolean = false
) {
    if (items.isEmpty()) return

    Spacer(modifier = Modifier.height(calculateRoundedValue(32).sdp))

    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        modifier = modifier.padding(horizontal = calculateRoundedValue(16).sdp),
        color = PrimaryText
    )

    Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp),
        contentPadding = PaddingValues(horizontal = calculateRoundedValue(16).sdp)
    ) {
        items(items) { item ->
            MediaItemCard(
                mediaItem = item,
                serverUrl = serverUrl,
                showProgress = showProgress,
                onClick = { onItemClick(it.id) }
            )
        }
    }
}