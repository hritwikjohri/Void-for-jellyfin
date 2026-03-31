package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun TmdbApiKeyDialog(
    initialKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var apiKey by remember(initialKey) { mutableStateOf(initialKey) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    VoidAlertDialog(
        visible = true,
        title = "TMDB API Key",
        icon = Icons.Default.VpnKey,
        onDismissRequest = onDismiss,
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        confirmButton = {
            Button(onClick = { onSave(apiKey.trim()) }) {
                Text("Save")
            }
        },
        content = {
            Column(
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
            ) {
                Text(
                    text = "Used to improve search matches when titles contain special characters.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .focusable(),
                    label = { Text("API Key") },
                    singleLine = true
                )
            }
        }
    )
}
