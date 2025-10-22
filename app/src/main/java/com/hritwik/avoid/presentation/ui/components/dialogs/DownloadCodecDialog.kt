package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import com.hritwik.avoid.domain.model.download.DownloadCodec
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun DownloadCodecDialog(
    options: List<DownloadCodec>,
    currentCodec: DownloadCodec,
    onCodecSelected: (DownloadCodec) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Preferred Codec",
        icon = Icons.Default.Tune,
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(options) { codec ->
                val subtitle = buildString {
                    append(codec.serverValue.uppercase())
                    codec.profile?.let { profile ->
                        append(" • ")
                        append(profile.uppercase())
                    }
                }
                SelectionItem(
                    title = codec.label,
                    subtitle = subtitle,
                    isSelected = codec == currentCodec,
                    onClick = { onCodecSelected(codec) }
                )
            }
        }
    }
}
