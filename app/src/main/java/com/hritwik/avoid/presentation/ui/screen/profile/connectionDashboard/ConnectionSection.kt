package com.hritwik.avoid.presentation.ui.screen.profile.connectionDashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun ConnectionSection(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    inputs: List<String>,
    onInputChange: (Int, String) -> Unit,
    onMove: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    sanitizedInputs: List<String>,
    savedConnections: List<String>,
    labelPrefix: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(calculateRoundedValue(16).sdp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(calculateRoundedValue(40).sdp)
                        .clip(RoundedCornerShape(calculateRoundedValue(8).sdp))
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(calculateRoundedValue(24).sdp)
                    )
                }
                Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

            inputs.forEachIndexed { index, value ->
                ConnectionInputRow(
                    value = value,
                    onValueChange = { onInputChange(index, it) },
                    label = "$labelPrefix ${index + 1}",
                    canMoveUp = index > 0,
                    canMoveDown = index < inputs.lastIndex,
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) },
                    onRemove = { onRemove(index) },
                    canRemove = inputs.size > 1 || value.isNotBlank()
                )
                if (index < inputs.lastIndex) {
                    Spacer(modifier = Modifier.height(calculateRoundedValue(12).sdp))
                }
            }

            Spacer(modifier = Modifier.height(calculateRoundedValue(12).sdp))
            TextButton(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(calculateRoundedValue(6).sdp))
                Text("Add ${title.lowercase()} address")
            }

            
            HorizontalDivider(
                modifier = Modifier.padding(vertical = calculateRoundedValue(12).sdp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
            ) {
                Button(
                    onClick = onSave,
                    enabled = sanitizedInputs.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(calculateRoundedValue(6).sdp))
                    Text("Save")
                }

                OutlinedButton(
                    onClick = onClear,
                    enabled = sanitizedInputs.isNotEmpty() || savedConnections.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(calculateRoundedValue(6).sdp))
                    Text("Clear All")
                }
            }
        }
    }
}