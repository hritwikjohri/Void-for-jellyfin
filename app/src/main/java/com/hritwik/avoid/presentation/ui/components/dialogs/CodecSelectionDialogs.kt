package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hevc
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hritwik.avoid.domain.model.playback.PreferredAudioCodec
import com.hritwik.avoid.domain.model.playback.PreferredVideoCodec
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun PreferredVideoCodecDialog(
    selectedCodec: PreferredVideoCodec,
    onCodecSelected: (PreferredVideoCodec) -> Unit,
    onDismiss: () -> Unit,
) {
    VoidAlertDialog(
        visible = true,
        title = stringResource(id = R.string.dialog_preferred_video_codec_title),
        icon = Icons.Default.Hevc,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                items(PreferredVideoCodec.entries) { codec ->
                    val detail = buildString {
                        append(codec.codecQueryValue.uppercase())
                        codec.profile?.let { profile ->
                            append(" • ")
                            append(profile.uppercase())
                        }
                    }
                    val label = if (detail.isNotEmpty()) {
                        "${codec.label} • $detail"
                    } else {
                        codec.label
                    }
                    SelectionItem(
                        title = label,
                        isSelected = codec == selectedCodec,
                        onClick = { onCodecSelected(codec) }
                    )
                }
            }
        }
    )
}

@Composable
fun PreferredAudioCodecDialog(
    selectedCodec: PreferredAudioCodec,
    onCodecSelected: (PreferredAudioCodec) -> Unit,
    onDismiss: () -> Unit,
) {
    VoidAlertDialog(
        visible = true,
        title = stringResource(id = R.string.dialog_preferred_audio_codec_title),
        icon = Icons.Filled.GraphicEq,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                items(PreferredAudioCodec.entries) { codec ->
                    val label = "${codec.label} • ${codec.preferenceValue.uppercase()}"
                    SelectionItem(
                        title = label,
                        isSelected = codec == selectedCodec,
                        onClick = { onCodecSelected(codec) }
                    )
                }
            }
        }
    )
}
