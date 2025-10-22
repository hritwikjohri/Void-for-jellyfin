package com.hritwik.avoid.presentation.ui.components.dialogs

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun StorageLocationDialog(
    currentLocation: String,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val locations = listOf("internal", "external")

    SelectionDialog(
        title = "Storage Location",
        onDismiss = onDismiss
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
            items(locations) { location ->
                SelectionItem(
                    title = if (location == "internal") "Internal" else "External",
                    subtitle = null,
                    isSelected = currentLocation == location,
                    onClick = { onLocationSelected(location) }
                )
            }
        }
    }
}