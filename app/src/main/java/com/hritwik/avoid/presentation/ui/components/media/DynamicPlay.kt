package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.UserData

private const val THRESHOLD = 10_000_000L

@Composable
fun DynamicPlayButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    dominantColor: Color,
    buttonSize: Dp,
    iconSize: Dp,
    mediaItem: MediaItem? = null,
    hasResumableProgress: Boolean = false
) {
    val animatedColor by animateColorAsState(
        targetValue = dominantColor,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "play_button_color"
    )

    val iconTint = if (animatedColor.luminance() > 0.5f) Color.Black else Color.White

    val animatedIconTint by animateColorAsState(
        targetValue = iconTint,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "play_button_icon_tint"
    )

    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "play_button_scale"
    )

    val isCompleted = mediaItem?.let { item ->
        val userData = item.userData
        val playbackPositionTicks = userData?.playbackPositionTicks ?: 0L
        val runTimeTicks = item.runTimeTicks
        (userData?.played == true) ||
            (runTimeTicks != null && playbackPositionTicks >= runTimeTicks - THRESHOLD)
    } ?: false
    val icon = if (isCompleted) Icons.Filled.Replay else Icons.Filled.PlayArrow
    val contentDescription = if (isCompleted) "Replay" else "Play"

    Box(modifier = modifier.size(buttonSize)) {
        FilledTonalButton(
            onClick = onClick,
            modifier = Modifier
                .matchParentSize()
                .scale(scale)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            tryAwaitRelease()
                            isPressed = false
                        }
                    )
                },
            shape = CircleShape,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = animatedColor,
                contentColor = animatedIconTint
            ),
            elevation = ButtonDefaults.filledTonalButtonElevation(
                defaultElevation = calculateRoundedValue(6).sdp,
                pressedElevation = calculateRoundedValue(2).sdp
            ),
            contentPadding = PaddingValues(calculateRoundedValue(0).sdp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(buttonSize)
            ) {
                if (hasResumableProgress && !isCompleted) {
                    Icon(
                        painter = painterResource(R.drawable.resume),
                        contentDescription = "Replay",
                        modifier = Modifier.size(iconSize),
                        tint = animatedIconTint
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = contentDescription,
                        modifier = Modifier.size(iconSize),
                        tint = animatedIconTint
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DynamicPlayButtonPreview() {
    val item = MediaItem(
        id = "1",
        name = "Sample",
        type = "Movie",
        overview = null,
        year = 2024,
        communityRating = null,
        runTimeTicks = 120L * 60 * 10_000_000L,
        primaryImageTag = null,
        logoImageTag = null,
        backdropImageTags = emptyList(),
        genres = emptyList(),
        isFolder = false,
        childCount = null,
        userData = UserData(playbackPositionTicks = 30L * 60 * 10_000_000L)
    )
    DynamicPlayButton(
        onClick = {},
        onResumeClick = {},
        dominantColor = Color.Blue,
        buttonSize = 72.dp,
        iconSize = 36.dp,
        mediaItem = item,
        hasResumableProgress = true
    )
}