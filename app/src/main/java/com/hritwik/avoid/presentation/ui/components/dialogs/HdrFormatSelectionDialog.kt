package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.runtime.Composable
import com.hritwik.avoid.domain.model.playback.HdrFormatPreference
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun HdrFormatSelectionDialog(
    currentPreference: HdrFormatPreference,
    onPreferenceSelected: (HdrFormatPreference) -> Unit,
    onDismiss: () -> Unit
) {
    VoidAlertDialog(
        visible = true,
        title = "HDR Format",
        icon = Icons.Default.HdrOn,
        onDismissRequest = onDismiss,
        dismissText = "Close",
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                val options = listOf(
                    HdrFormatPreference.AUTO,
                    HdrFormatPreference.HDR10_PLUS,
                    HdrFormatPreference.DOLBY_VISION
                )
                items(options) { preference ->
                    SelectionItem(
                        title = preference.displayName,
                        isSelected = currentPreference == preference,
                        onClick = { onPreferenceSelected(preference) }
                    )
                }
            }
        }
    )
}
