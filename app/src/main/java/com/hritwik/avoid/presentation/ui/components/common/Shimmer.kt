package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

/**
 * Shimmer effect for loading skeletons
 */
@Composable
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(calculateRoundedValue(12).sdp)
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerColors = listOf(
        Color.Gray.copy(alpha = 0.3f),
        Color.Gray.copy(alpha = 0.5f),
        Color.Gray.copy(alpha = 0.3f),
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim.value - 500f, translateAnim.value - 500f),
            end = Offset(translateAnim.value, translateAnim.value)
        ),
        shape = shape
    )
}
