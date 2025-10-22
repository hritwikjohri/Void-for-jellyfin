package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.media.VideoQuality
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.PlaybackTranscodeOption
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun VersionSelectionDialog(
    versions: List<MediaSource>,
    selectedVersion: MediaSource?,
    onVersionSelected: (MediaSource) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Select Version",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(versions) { version ->
                SelectionItem(
                    title = version.displayName,
                    subtitle = buildString {
                        version.container?.let { append(it.uppercase()) }
                        version.size?.let {
                            if (isNotEmpty()) append(" • ")
                            append(formatFileSize(it))
                        }
                        version.bitrate?.let {
                            if (isNotEmpty()) append(" • ")
                            append("${it / 1000} kbps")
                        }
                    },
                    isSelected = selectedVersion?.id == version.id,
                    onClick = { onVersionSelected(version) }
                )
            }
        }
    }
}

@Composable
fun VideoQualityDialog(
    qualities: List<VideoQuality>,
    selectedQuality: VideoQuality?,
    onQualitySelected: (VideoQuality) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Video Quality",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(qualities) { quality ->
                SelectionItem(
                    title = quality.displayName,
                    subtitle = "${quality.height}p",
                    isSelected = selectedQuality == quality,
                    onClick = { onQualitySelected(quality) }
                )
            }
        }
    }
}

@Composable
fun PlaybackTranscodeDialog(
    options: List<PlaybackTranscodeOption>,
    selectedOption: PlaybackTranscodeOption,
    onOptionSelected: (PlaybackTranscodeOption) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = stringResource(id = R.string.dialog_playback_quality_title),
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(options) { option ->
                val subtitle = if (option.isOriginal) {
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
                SelectionItem(
                    title = option.label,
                    subtitle = subtitle,
                    isSelected = option == selectedOption,
                    onClick = { onOptionSelected(option) }
                )
            }
        }
    }
}

@Composable
fun AudioTrackDialog(
    audioStreams: List<MediaStream>,
    selectedAudioStream: MediaStream?,
    onAudioSelected: (MediaStream) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Audio Track",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(audioStreams) { audioStream ->
                SelectionItem(
                    title = audioStream.audioDescription,
                    subtitle = buildString {
                        audioStream.codec?.let { append(it.uppercase()) }
                        audioStream.bitRate?.let {
                            if (isNotEmpty()) append(" • ")
                            append("${it / 1000} kbps")
                        }
                        if (audioStream.isDefault) {
                            if (isNotEmpty()) append(" • ")
                            append("Default")
                        }
                    },
                    isSelected = selectedAudioStream?.index == audioStream.index,
                    onClick = { onAudioSelected(audioStream) }
                )
            }
        }
    }
}

@Composable
fun SubtitleDialog(
    subtitleStreams: List<MediaStream>,
    selectedSubtitleStream: MediaStream?,
    onSubtitleSelected: (MediaStream?) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Subtitles",
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            
            item {
                SelectionItem(
                    title = "Off",
                    subtitle = "No subtitles",
                    isSelected = selectedSubtitleStream == null,
                    onClick = { onSubtitleSelected(null) }
                )
            }

            
            items(subtitleStreams) { subtitleStream ->
                SelectionItem(
                    title = subtitleStream.subtitleDescription,
                    subtitle = buildString {
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
                    },
                    isSelected = selectedSubtitleStream?.index == subtitleStream.index,
                    onClick = { onSubtitleSelected(subtitleStream) }
                )
            }
        }
    }
}

@Composable
fun DisplayModeSelectionDialog(
    currentMode: DisplayMode,
    onModeSelected: (DisplayMode) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = "Display Mode",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(DisplayMode.entries) { mode ->
                SelectionItem(
                    title = mode.value,
                    subtitle = mode.description,
                    isSelected = currentMode == mode,
                    onClick = { onModeSelected(mode) }
                )
            }
        }
    }
}

@Composable
fun SpeedSelectionDialog(
    currentSpeed: Float,
    onSpeedSelected: (Float) -> Unit,
    onDismiss: () -> Unit
){
    val speeds = listOf(0.25f,0.5f,0.75f,1.0f,1.25f,1.5f,1.75f,2.0f)
    SelectionDialog(
        title = "Player Speed",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(speeds) { speeds ->
                SelectionItem(
                    title = speeds.toString(),
                    subtitle = "x",
                    isSelected = currentSpeed == speeds,
                    onClick = { onSpeedSelected(speeds) }
                )
            }
        }
    }
}