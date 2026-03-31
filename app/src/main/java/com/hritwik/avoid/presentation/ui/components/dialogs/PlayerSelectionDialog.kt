package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
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
    VoidAlertDialog(
        visible = true,
        title = stringResource(R.string.player_selection_title),
        icon = Icons.Default.SmartDisplay,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
            ) {
                items(PlayerType.entries) { playerType ->
                    SelectionItem(
                        title = playerType.value,
                        isSelected = currentPlayer == playerType,
                        onClick = { onPlayerSelected(playerType) }
                    )
                }
            }
        }
    )
}
