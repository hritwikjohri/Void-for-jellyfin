package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.R
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun PlayerSelectionDialog(
    currentPlayer: PlayerType,
    onPlayerSelected: (PlayerType) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionDialog(
        title = stringResource(R.string.player_selection_title),
        onDismiss = onDismiss
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
        ) {
            items(PlayerType.entries) { playerType ->
                PlayerSelectionItem(
                    playerType = playerType,
                    isSelected = currentPlayer == playerType,
                    onClick = { onPlayerSelected(playerType) }
                )
            }
        }
    }
}

@Composable
private fun PlayerSelectionItem(
    playerType: PlayerType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (playerType) {
        PlayerType.MPV -> Icons.Default.SmartDisplay
        PlayerType.EXOPLAYER -> Icons.Default.Videocam
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            ButtonDefaults.outlinedButtonBorder.copy(
                width = calculateRoundedValue(2).sdp
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(calculateRoundedValue(16).sdp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                },
                modifier = Modifier.size(calculateRoundedValue(24).sdp)
            )

            Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = playerType.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )

                Spacer(modifier = Modifier.height(calculateRoundedValue(4).sdp))

                Text(
                    text = playerType.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    }
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(calculateRoundedValue(12).sdp))
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.selected),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(calculateRoundedValue(20).sdp)
                )
            }
        }
    }
}
