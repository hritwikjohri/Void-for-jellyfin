package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.rounded.VideoSettings
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.media.VideoQuality
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.PlaybackTranscodeOption
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.ui.theme.normalizeHexColor
import com.hritwik.avoid.utils.constants.PreferenceConstants
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun VersionSelectionDialog(
    versions: List<MediaSource>,
    selectedVersion: MediaSource?,
    onVersionSelected: (MediaSource) -> Unit,
    onDismiss: () -> Unit,
    selectedColor: Color? = null
) {
    VoidAlertDialog(
        visible = true,
        title = "Select Version",
        icon = Icons.Default.Settings,
        borderColor = selectedColor ?: Minsk,
        actionColor = selectedColor ?: Minsk,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                items(versions) { version ->
                    val detail = buildString {
                        version.container?.let { append(it.uppercase()) }
                        version.size?.let {
                            if (isNotEmpty()) append(" • ")
                            append(formatFileSize(it))
                        }
                        version.bitrate?.let {
                            if (isNotEmpty()) append(" • ")
                            append(String.format("%.1f Mbps", it / 1_000_000f))
                        }
                    }
                    val label = if (detail.isNotEmpty()) {
                        "${version.displayName} • $detail"
                    } else {
                        version.displayName
                    }
                    SelectionItem(
                        title = label,
                        isSelected = selectedVersion?.id == version.id,
                        selectedColor = selectedColor,
                        onClick = { onVersionSelected(version) }
                    )
                }
            }
        }
    )
}

@Composable
fun VideoQualityDialog(
    qualities: List<VideoQuality>,
    selectedQuality: VideoQuality?,
    onQualitySelected: (VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    VoidAlertDialog(
        visible = true,
        title = "Video Quality",
        onDismissRequest = onDismiss,
        icon = Icons.Rounded.VideoSettings,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                items(qualities) { quality ->
                    val label = "${quality.displayName} • ${quality.height}p"
                    SelectionItem(
                        title = label,
                        isSelected = selectedQuality == quality,
                        onClick = { onQualitySelected(quality) }
                    )
                }
            }
        }
    )
}

@Composable
fun PlaybackTranscodeDialog(
    options: List<PlaybackTranscodeOption>,
    selectedOption: PlaybackTranscodeOption,
    onOptionSelected: (PlaybackTranscodeOption) -> Unit,
    onDismiss: () -> Unit
) {
    VoidAlertDialog(
        visible = true,
        title = stringResource(id = R.string.dialog_playback_quality_title),
        onDismissRequest = onDismiss,
        icon = Icons.Default.TrackChanges,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                items(options) { option ->
                    val detail = if (option.isOriginal) {
                        stringResource(id = R.string.dialog_playback_quality_original)
                    } else {
                        buildString {
                            option.displayHeight?.let { append("${it}p") }
                            option.displayBitrate?.let { bitrate ->
                                if (isNotEmpty()) append(" • ")
                                append("${bitrate / 1_000_000} Mbps")
                            }
                        }
                    }
                    val label = if (detail.isNotEmpty()) {
                        "${option.label} • $detail"
                    } else {
                        option.label
                    }
                    SelectionItem(
                        title = label,
                        isSelected = option == selectedOption,
                        onClick = { onOptionSelected(option) }
                    )
                }
            }
        }
    )
}

@Composable
fun AudioTrackDialog(
    audioStreams: List<MediaStream>,
    selectedAudioStream: MediaStream?,
    onAudioSelected: (MediaStream) -> Unit,
    onDismiss: () -> Unit,
    selectedColor: Color? = null
) {
    VoidAlertDialog(
        visible = true,
        title = "Audio Track",
        icon = Icons.Filled.Audiotrack,
        borderColor = selectedColor ?: Minsk,
        actionColor = selectedColor ?: Minsk,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                items(audioStreams) { audioStream ->
                    val detail = buildString {
                        audioStream.codec?.let { append(it.uppercase()) }
                        audioStream.bitRate?.let {
                            if (isNotEmpty()) append(" • ")
                            append("${it / 1000} kbps")
                        }
                        if (audioStream.isDefault) {
                            if (isNotEmpty()) append(" • ")
                            append("Default")
                        }
                    }
                    val label = if (detail.isNotEmpty()) {
                        "${audioStream.audioDescription} • $detail"
                    } else {
                        audioStream.audioDescription
                    }
                    SelectionItem(
                        title = label,
                        isSelected = selectedAudioStream?.index == audioStream.index,
                        selectedColor = selectedColor,
                        onClick = { onAudioSelected(audioStream) }
                    )
                }
            }
        }
    )
}

@Composable
fun SubtitleDialog(
    subtitleStreams: List<MediaStream>,
    selectedSubtitleStream: MediaStream?,
    onSubtitleSelected: (MediaStream?) -> Unit,
    onDismiss: () -> Unit,
    selectedColor: Color? = null
) {
    VoidAlertDialog(
        visible = true,
        title = "Subtitles",
        onDismissRequest = onDismiss,
        icon = Icons.Filled.Subtitles,
        borderColor = selectedColor ?: Minsk,
        actionColor = selectedColor ?: Minsk,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {

                item {
                    SelectionItem(
                        title = "Off (No subtitles)",
                        isSelected = selectedSubtitleStream == null,
                        selectedColor = selectedColor,
                        onClick = { onSubtitleSelected(null) }
                    )
                }


                items(subtitleStreams) { subtitleStream ->
                    val detail = buildString {
                        subtitleStream.codec?.let { append(it.uppercase()) }
                        if (subtitleStream.isDefault) {
                            if (isNotEmpty()) append(" • ")
                            append("Default")
                        }
                        if (subtitleStream.isForced) {
                            if (isNotEmpty()) append(" • ")
                            append("Forced")
                        }
                        if (subtitleStream.isExternal) {
                            if (isNotEmpty()) append(" • ")
                            append("External")
                        }
                    }
                    val label = if (detail.isNotEmpty()) {
                        "${subtitleStream.displayTitle.orEmpty()} • $detail"
                    } else {
                        subtitleStream.displayTitle.orEmpty()
                    }
                    SelectionItem(
                        title = label,
                        isSelected = selectedSubtitleStream?.index == subtitleStream.index,
                        selectedColor = selectedColor,
                        onClick = { onSubtitleSelected(subtitleStream) }
                    )
                }
            }
        }
    )
}

@Composable
fun PlayerProgressColorDialog(
    currentColorKey: String,
    currentSeekColorKey: String,
    onColorSaved: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val existingHex = normalizeHexColor(currentColorKey) ?: ""
    var input by remember(existingHex) { mutableStateOf(existingHex) }
    val existingSeekHex = normalizeHexColor(currentSeekColorKey) ?: ""
    var seekInput by remember(existingSeekHex) { mutableStateOf(existingSeekHex) }
    val focusRequester = remember { FocusRequester() }
    val normalized = normalizeHexColor(input)
    val seekNormalized = normalizeHexColor(seekInput)
    val isValid = input.trim().isEmpty() || normalized != null
    val isSeekValid = seekInput.trim().isEmpty() || seekNormalized != null

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    VoidAlertDialog(
        visible = true,
        title = "Progress Bar Color",
        onDismissRequest = onDismiss,
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(
                enabled = isValid && isSeekValid,
                onClick = {
                    val value = if (input.trim().isEmpty()) {
                        PreferenceConstants.DEFAULT_PLAYER_PROGRESS_COLOR
                    } else {
                        normalized ?: currentColorKey
                    }
                    val seekValue = if (seekInput.trim().isEmpty()) {
                        PreferenceConstants.DEFAULT_PLAYER_PROGRESS_SEEK_COLOR
                    } else {
                        seekNormalized ?: currentSeekColorKey
                    }
                    onColorSaved(value, seekValue)
                }
            ) {
                Text("Save")
            }
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
            ) {
                Text(
                    text = "Enter a hex color like #RRGGBB or #AARRGGBB.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .focusable(),
                    label = { Text("Hex color") },
                    singleLine = true,
                    isError = !isValid
                )

                OutlinedTextField(
                    value = seekInput,
                    onValueChange = { seekInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable(),
                    label = { Text("Seek color (optional)") },
                    singleLine = true,
                    isError = !isSeekValid
                )
            }
        }
    )
}

@Composable
fun DisplayModeSelectionDialog(
    currentMode: DisplayMode,
    onModeSelected: (DisplayMode) -> Unit,
    onDismiss: () -> Unit
) {
    VoidAlertDialog(
        visible = true,
        title = "Display Mode",
        icon = Icons.Default.Monitor,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                items(DisplayMode.entries) { mode ->
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

@Composable
fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
){
    val speeds = listOf(0.25f,0.5f,0.75f,1.0f,1.25f,1.5f,1.75f,2.0f)
    VoidAlertDialog(
        visible = true,
        title = "Player Speed",
        onDismissRequest = onDismiss,
        icon = Icons.Filled.Speed,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                items(speeds) { speed ->
                    SelectionItem(
                        title = "${speed}x",
                        isSelected = currentSpeed == speed,
                        onClick = { onSpeedSelected(speed) }
                    )
                }
            }
        }
    )
}
