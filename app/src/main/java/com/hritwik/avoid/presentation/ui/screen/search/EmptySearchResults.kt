package com.hritwik.avoid.presentation.ui.screen.search

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.presentation.ui.theme.PrimaryText

@Composable
fun EmptySearchResults(
    query: String,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(calculateRoundedValue(200).sdp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No results found for $query",
                style = MaterialTheme.typography.headlineSmall,
                color = PrimaryText
            )
            Text(
                text = "Try adjusting your search terms or filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = calculateRoundedValue(8).sdp)
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(top = calculateRoundedValue(16).sdp)
            ) {
                Text(
                    text = "Browse Categories Instead",
                    color = PrimaryText
                )
            }
        }
    }
}