package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ir.kaaveh.sdpcompose.sdp
import com.hritwik.avoid.utils.helpers.calculateRoundedValue

@Composable
fun MpvConfigDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var configText by remember { mutableStateOf(initialValue) }

    LaunchedEffect(initialValue) {
        configText = initialValue
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "MPV Config", style = MaterialTheme.typography.titleLarge) },
        text = {
            OutlinedTextField(
                value = configText,
                onValueChange = { configText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = calculateRoundedValue(200).sdp),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = false,
                maxLines = Int.MAX_VALUE
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(configText) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
