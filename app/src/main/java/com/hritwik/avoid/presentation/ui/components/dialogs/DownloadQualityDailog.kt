package com.hritwik.avoid.presentation.ui.components.dialogs

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.download.DownloadQuality

@Composable
fun DownloadQualityDialog(
    options: List<DownloadQuality>,
    currentQuality: DownloadQuality,
    onQualitySelected: (DownloadQuality) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Download Quality",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(options) { quality ->
                SelectionItem(
                    title = quality.label,
                    subtitle = "${quality.maxBitrate / 1_000_000} Mbps",
                    isSelected = currentQuality == quality,
                    onClick = { onQualitySelected(quality) }
                )
            }
        }
    }
}