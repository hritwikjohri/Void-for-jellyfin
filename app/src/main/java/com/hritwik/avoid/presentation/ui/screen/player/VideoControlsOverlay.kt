package com.hritwik.avoid.presentation.ui.screen.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.presentation.ui.components.dialogs.SpeedSelectionDialog
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.ui.theme.SeekBarBackground
import com.hritwik.avoid.utils.extensions.formatTime
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoControlsOverlay(
    mediaTitle: String,
    isPlaying: Boolean,
    isBuffering: Boolean,
    duration: Long,
    currentPosition: Long,
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekComplete: (Long) -> Unit,
    onBackClick: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    skipButtonVisible: Boolean = false,
    skipButtonLabel: String = "",
    onSkipButtonClick: (() -> Unit)? = null,
    onPlayPrevious: (() -> Unit)? = null,
    onPlayNext: (() -> Unit)? = null,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    playbackQualityLabel: String,
    onPlaybackQualityClick: (() -> Unit)? = null,
    onAudioClick: (() -> Unit)? = null,
    onSubtitleClick: (() -> Unit)? = null,
    onDecoderClick: (() -> Unit)? = null,
    onDisplayClick: (() -> Unit)? = null,
    onPipClick: (() -> Unit)? = null
) {
    var showSpeedDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
    ) {
        if (showSpeedDialog) {
            SpeedSelectionDialog(
                currentSpeed = speed,
                onSpeedSelected = { newSpeed ->
                    onSpeedChange(newSpeed)
                    showSpeedDialog = false
                },
                onDismiss = { showSpeedDialog = false }
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(calculateRoundedValue(16).sdp)
                .statusBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Text(
                text = mediaTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            
            TextButton(onClick = { showSpeedDialog = true }) {
                Text(text = "${speed}x", color = Color.White)
            }

            TextButton(
                onClick = { onPlaybackQualityClick?.invoke() },
                enabled = onPlaybackQualityClick != null
            ) {
                Text(
                    text = playbackQualityLabel,
                    color = if (onPlaybackQualityClick != null) Color.White else Color.Gray
                )
            }

            onAudioClick?.let { handler ->
                IconButton(onClick = handler) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Audio tracks change",
                        tint = Color.White
                    )
                }
            }

            onSubtitleClick?.let { handler ->
                IconButton(onClick = handler) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = "Subtitles change",
                        tint = Color.White
                    )
                }
            }

            onDisplayClick?.let { handler ->
                IconButton(onClick = handler) {
                    Icon(
                        imageVector = Icons.Default.DisplaySettings,
                        contentDescription = "Display change",
                        tint = Color.White
                    )
                }
            }

            onDecoderClick?.let { handler ->
                IconButton(onClick = handler) {
                    Icon(
                        imageVector = Icons.Default.VideoSettings,
                        contentDescription = "Decoder mode change",
                        tint = Color.White
                    )
                }
            }

            onPipClick?.let { handler ->
                IconButton(onClick = handler) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Picture-in-picture",
                        tint = Color.White
                    )
                }
            }
        }

        
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(48).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onPlayPrevious?.invoke() },
                enabled = onPlayPrevious != null,
                modifier = Modifier.size(calculateRoundedValue(48).sdp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Next episode",
                    tint = if (onPlayPrevious == null) Color.Gray else Color.White,
                    modifier = Modifier.size(calculateRoundedValue(36).sdp)
                )
            }


            IconButton(
                onClick = onSkipBackward,
                modifier = Modifier.size(calculateRoundedValue(48).sdp)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay10,
                    contentDescription = "Skip backward 10 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(calculateRoundedValue(36).sdp)
                )
            }

            IconButton(
                onClick = onPlayPauseClick,
                modifier = Modifier.size(calculateRoundedValue(64).sdp)
            ) {
                when {
                    isBuffering -> {
                        null
                    }
                    isPlaying -> {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(calculateRoundedValue(48).sdp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(calculateRoundedValue(48).sdp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onSkipForward,
                modifier = Modifier.size(calculateRoundedValue(48).sdp)
            ) {
                Icon(
                    imageVector = Icons.Default.Forward10,
                    contentDescription = "Skip forward 10 seconds",
                    tint = Color.White,
                    modifier = Modifier.size(calculateRoundedValue(36).sdp)
                )
            }

            IconButton(
                onClick = { onPlayNext?.invoke() },
                enabled = onPlayNext != null,
                modifier = Modifier.size(calculateRoundedValue(48).sdp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next episode",
                    tint = if (onPlayNext == null) Color.Gray else Color.White,
                    modifier = Modifier.size(calculateRoundedValue(36).sdp)
                )
            }

        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = calculateRoundedValue(16).sdp, vertical = calculateRoundedValue(24).sdp)
                .navigationBarsPadding()
        ) {
            if (skipButtonVisible && onSkipButtonClick != null) {
                TextButton(
                    onClick = onSkipButtonClick,
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.White.copy(alpha = 0.2f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(text = skipButtonLabel)
                }
            }

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatTime(currentPosition),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
                Text(
                    text = formatTime(duration),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )
            }

            LinearProgressIndicator(
                progress = {
                    if (duration > 0L) currentPosition.toFloat() / duration.toFloat() else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = calculateRoundedValue(8).sdp),
                trackColor = SeekBarBackground,
                color = Minsk
            )
        }
    }
}