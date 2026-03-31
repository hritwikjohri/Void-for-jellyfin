package com.hritwik.avoid.presentation.ui.components.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import ir.kaaveh.sdpcompose.sdp
import com.hritwik.avoid.R
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

    VoidAlertDialog(
        visible = true,
        title = stringResource(R.string.mpv_config_title),
        onDismissRequest = onDismiss,
        icon = Icons.Default.Code,
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.cancel))
            }
        },
        confirmButton = {
            Button(onClick = { onSave(configText) }) {
                Text(text = stringResource(R.string.save))
            }
        },
        content = {
            OutlinedTextField(
                value = configText,
                onValueChange = { configText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = calculateRoundedValue(200).sdp),
                textStyle = MaterialTheme.typography.bodyMedium,
                singleLine = false,
                maxLines = Int.MAX_VALUE,
                shape = RoundedCornerShape(calculateRoundedValue(24).sdp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.6f),
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
        }
    )
}
