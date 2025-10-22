package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.ssp

@Composable
fun SubtleShinySignature(
    modifier: Modifier
) {
    val transition = rememberInfiniteTransition(label = "shine")
    val shift by transition.animateFloat(
        initialValue = -200f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shift"
    )

    val shineBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.35f),
            Color.White.copy(alpha = 0.85f),
            Color.White.copy(alpha = 0.85f),
            Color.White.copy(alpha = 0.35f),
        ),
        start = Offset(shift - 200f, 0f),
        end = Offset(shift + 200f, 0f)
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = "by ऋत्विक",
            style = TextStyle(
                fontFamily = FontFamily(Font(R.font.kalam_bold)),
                fontWeight = FontWeight.Bold,
                fontSize = calculateRoundedValue(15).ssp,
                brush = shineBrush,
                shadow = Shadow(
                    color = Color.White.copy(alpha = 0.2f),
                    offset = Offset.Zero,
                    blurRadius = 5f
                )
            )
        )
    }
}
