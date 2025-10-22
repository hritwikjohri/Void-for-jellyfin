package com.hritwik.avoid.presentation.ui.components.common

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.hritwik.avoid.R
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun LogoutDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean
) {
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = stringResource(R.string.sign_out),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Text(
                text = stringResource(R.string.sign_out_confirm_message),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            val confirmFocusRequester = remember { FocusRequester() }
            Button(
                onClick = onConfirm,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier
                    .focusRequester(confirmFocusRequester)
                    .semantics { role = Role.Button }
                    .focusable()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(calculateRoundedValue(16).sdp),
                        color = MaterialTheme.colorScheme.onError,
                        strokeWidth = calculateRoundedValue(2).sdp
                    )
                } else {
                    Text(stringResource(R.string.sign_out))
                }
            }
        },
        dismissButton = {
            val dismissFocusRequester = remember { FocusRequester() }
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier
                    .focusRequester(dismissFocusRequester)
                    .semantics { role = Role.Button }
                    .focusable()
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}