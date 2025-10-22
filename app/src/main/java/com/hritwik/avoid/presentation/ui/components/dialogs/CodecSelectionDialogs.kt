package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    SelectionDialog(
        title = stringResource(id = R.string.dialog_preferred_video_codec_title),
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(PreferredVideoCodec.entries) { codec ->
                val subtitle = buildString {
                    append(codec.codecQueryValue.uppercase())
                    codec.profile?.let { profile ->
                        append(" • ")
                        append(profile.uppercase())
                    }
                }
                SelectionItem(
                    title = codec.label,
                    subtitle = subtitle,
                    isSelected = codec == selectedCodec,
                    onClick = { onCodecSelected(codec) }
                )
            }
        }
    }
}

@Composable
fun PreferredAudioCodecDialog(
    selectedCodec: PreferredAudioCodec,
    onCodecSelected: (PreferredAudioCodec) -> Unit,
    onDismiss: () -> Unit,
) {
    SelectionDialog(
        title = stringResource(id = R.string.dialog_preferred_audio_codec_title),
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(PreferredAudioCodec.entries) { codec ->
                SelectionItem(
                    title = codec.label,
                    subtitle = codec.preferenceValue.uppercase(),
                    isSelected = codec == selectedCodec,
                    onClick = { onCodecSelected(codec) }
                )
            }
        }
    }
}
