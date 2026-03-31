package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun StorageLocationDialog(
    currentLocation: String,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val locations = listOf("internal", "external")

    VoidAlertDialog(
        visible = true,
        title = "Storage Location",
        icon = Icons.Default.Storage,
        onDismissRequest = onDismiss,
        dismissText = stringResource(id = R.string.close),
        onDismissButton = onDismiss,
        content = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                items(locations) { location ->
                    SelectionItem(
                        title = if (location == "internal") "Internal" else "External",
                        isSelected = currentLocation == location,
                        onClick = { onLocationSelected(location) }
                    )
                }
            }
        }
    )
}