package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import com.hritwik.avoid.R

@Composable
fun DecoderSelectionDialog(
    currentMode: DecoderMode,
    onModeSelected: (DecoderMode) -> Unit,
    onDismiss: () -> Unit
) {
    VoidAlertDialog(
        visible = true,
        title = "Video Decoder",
        icon = Icons.Default.VideoSettings,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                items(DecoderMode.entries) { mode ->
                    SelectionItem(
                        title = mode.value,
                        isSelected = currentMode == mode,
                        onClick = { onModeSelected(mode) }
                    )
                }
            }
        }
    )
}