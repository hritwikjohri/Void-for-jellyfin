package com.hritwik.avoid.presentation.ui.components.media

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.hritwik.avoid.presentation.ui.theme.PrimaryText

@Composable
fun OverviewSection(
    overview: String,
    modifier: Modifier = Modifier,
    title: String = "Plot",
    maxLinesCollapsed: Int = 3,
    tagline: String? = null
) {
    var isExpanded by remember { mutableStateOf(false) }
    val maxLines = if (isExpanded) Int.MAX_VALUE else maxLinesCollapsed

    Column(modifier = modifier) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = PrimaryText
        )

        Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

        if (!tagline.isNullOrBlank()) {
            Text(
                text = tagline,
                style = MaterialTheme.typography.bodyLarge,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                color = PrimaryText.copy(alpha = 0.9f),
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
            )
            if (overview.isNotBlank()) {
                Spacer(modifier = Modifier.height(calculateRoundedValue(12).sdp))
            }
        }

        if (overview.isNotBlank()) {
            Text(
                text = overview,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable (
                    onClick = { isExpanded = !isExpanded },
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ),
                color = PrimaryText
            )
        }
    }
}