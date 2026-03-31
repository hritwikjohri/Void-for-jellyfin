package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.presentation.ui.components.dialogs.VoidAlertDialog
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import com.hritwik.avoid.R

@Composable
fun DownloadQualityDialog(
    options: List<DownloadQuality>,
    currentQuality: DownloadQuality,
    onQualitySelected: (DownloadQuality) -> Unit,
    onDismiss: () -> Unit
) {
    VoidAlertDialog(
        visible = true,
        title = "Download Quality",
        icon = Icons.Default.CloudDownload,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                items(options) { quality ->
                    val label = buildString {
                        append(quality.label)
                        append(" • ")
                        append("${quality.maxBitrate / 1_000_000} Mbps")
                    }
                    SelectionItem(
                        title = label,
                        isSelected = currentQuality == quality,
                        onClick = { onQualitySelected(quality) }
                    )
                }
            }
        }
    )
}