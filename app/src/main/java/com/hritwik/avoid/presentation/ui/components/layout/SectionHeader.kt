package com.hritwik.avoid.presentation.ui.components.layout

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.presentation.ui.theme.PrimaryText

@Composable
fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String? = null,
    actionButton: @Composable () -> Unit = {},
    subtitle: String? = null,
    content: @Composable () -> Unit = {}
) {
    Column {
        Row(
            modifier = modifier.fillMaxWidth().padding(bottom = calculateRoundedValue(10).sdp, start = calculateRoundedValue(10).sdp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryText,
                    )
                }

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryText,
                    )
                }
            }

            actionButton()
        }

        content()
    }
}