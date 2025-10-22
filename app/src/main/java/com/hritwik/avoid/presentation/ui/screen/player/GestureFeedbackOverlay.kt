package com.hritwik.avoid.presentation.ui.screen.player

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.ssp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.hritwik.avoid.utils.extensions.formatTime

sealed class GestureFeedback {
    data class Text(val message: String) : GestureFeedback()
    data class Volume(val percent: Int) : GestureFeedback()
    data class Brightness(val percent: Int) : GestureFeedback()
    data class Seek(val position: Long) : GestureFeedback()
}

@Composable
fun GestureFeedbackOverlay(
    feedback: GestureFeedback?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = feedback != null,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut(),
        modifier = modifier
    ) {
        val message = when (feedback) {
            is GestureFeedback.Text -> feedback.message
            is GestureFeedback.Seek -> "Seek: ${formatTime(feedback.position)}"
            is GestureFeedback.Volume -> {
                val animated by animateIntAsState(
                    targetValue = feedback.percent,
                    label = "volumeAnim"
                )
                "Volume: ${animated}%"
            }
            is GestureFeedback.Brightness -> {
                val animated by animateIntAsState(
                    targetValue = feedback.percent,
                    label = "brightnessAnim"
                )
                "Brightness: ${animated}%"
            }
            null -> ""
        }

        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message,
                color = Color.White,
                overflow = TextOverflow.Visible,
                fontSize = calculateRoundedValue(24).ssp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}