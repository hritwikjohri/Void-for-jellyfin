package com.hritwik.avoid.presentation.ui.screen.profile.tvlogin

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.core.net.toUri
import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.common.ServerInfoRow
import com.hritwik.avoid.presentation.ui.components.common.SubtleShinySignature
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun TvLoginScreen(
    isOffline: Boolean,
    isWifiConnected: Boolean,
    activeConnection: ServerConnectionMethod?,
    sendAddress: String,
    isSending: Boolean,
    sendFeedback: String?,
    sendSuccess: Boolean?,
    selectedFileName: String?,
    serverPushPassword: String,
    onSendAddressChange: (String) -> Unit,
    onFileSelected: (Context, Uri?) -> Unit,
    onFileClear: () -> Unit,
    onPasswordChange: (String) -> Unit,
    onSendClick: (Context) -> Unit,
    onScanClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    val serverActionsEnabled = !isOffline
    val offlineMessage = "Reconnect to manage server settings"
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    val filePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        onFileSelected(context, uri)
    }

    AnimatedAmbientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .imePadding()
        ) {
            ScreenHeader(
                title = "Void TV Login",
                showBackButton = true,
                onBackClick = onBackClick
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = calculateRoundedValue(16).sdp),
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(
                        width = calculateRoundedValue(1).sdp,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                            Text(
                                text = "Connection Status",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        ServerInfoRow("Status", if (isOffline) "Offline" else "Online")
                        ServerInfoRow(
                            "Wi-Fi",
                            if (isWifiConnected) "Connected" else "Not connected"
                        )
                        val activeConnectionLabel = activeConnection?.let { method ->
                            val connectionLabel = if (method.isLocal) "Local" else "Internet"
                            "${method.url} ($connectionLabel)"
                        } ?: "Not available"
                        ServerInfoRow(
                            label = "Active Connection",
                            value = activeConnectionLabel
                        )
                        if (!serverActionsEnabled) {
                            Text(
                                text = offlineMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(
                        width = calculateRoundedValue(1).sdp,
                        color = Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
                        ) {
                            Text(
                                text = "TV Login",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Scan the QR to Login",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = sendAddress,
                            onValueChange = onSendAddressChange,
                            label = { Text("Destination IP: Port") },
                            placeholder = { Text("e.g. 192.168.0.10:5000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            shape = RoundedCornerShape(calculateRoundedValue(28).sdp),
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(calculateRoundedValue(8).sdp)
                        ) {
                            OutlinedButton(
                                onClick = onScanClick,
                                enabled = !isSending,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.QrCodeScanner,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(calculateRoundedValue(4).sdp))
                                Text(text = "Scan")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (serverActionsEnabled) {
                                        filePickerLauncher.launch("*/*")
                                    } else {
                                        Toast.makeText(context, offlineMessage, Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                                enabled = !isSending,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AttachFile,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(calculateRoundedValue(4).sdp))
                                Text(text = "Upload Cert")
                            }
                        }

                        if (!selectedFileName.isNullOrBlank()) {
                            OutlinedTextField(
                                value = serverPushPassword,
                                onValueChange = onPasswordChange,
                                label = { Text("Password") },
                                placeholder = { Text("Enter password") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = selectedFileName,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                TextButton(
                                    onClick = onFileClear,
                                    enabled = !isSending
                                ) {
                                    Text(text = "Remove")
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (serverActionsEnabled) {
                                    onSendClick(context)
                                } else {
                                    Toast.makeText(context, offlineMessage, Toast.LENGTH_SHORT)
                                        .show()
                                }
                            },
                            enabled = sendAddress.isNotBlank() && !isSending,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(calculateRoundedValue(18).sdp),
                                    strokeWidth = calculateRoundedValue(2).sdp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                                Text(text = "Send")
                            }
                        }

                        if (!sendFeedback.isNullOrBlank()) {
                            val feedbackColor = if (sendSuccess == true) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            Text(
                                text = sendFeedback,
                                style = MaterialTheme.typography.bodySmall,
                                color = feedbackColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                SubtleShinySignature(
                    modifier = Modifier.padding(bottom = calculateRoundedValue(90).sdp)
                )
            }
        }
    }
}

internal fun extractIpPort(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) {
        return null
    }

    val uri = runCatching { trimmed.toUri() }.getOrNull()
    if (uri != null) {
        val host = uri.host
        val port = uri.port.takeIf { it != -1 }
        if (!host.isNullOrBlank() && port != null) {
            return "$host:$port"
        }
    }

    val schemeRemoved = when {
        trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
        trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
        trimmed.startsWith("tcp://", ignoreCase = true) -> trimmed.substring(6)
        trimmed.startsWith("udp://", ignoreCase = true) -> trimmed.substring(6)
        trimmed.startsWith("ws://", ignoreCase = true) -> trimmed.substring(5)
        trimmed.startsWith("wss://", ignoreCase = true) -> trimmed.substring(6)
        else -> trimmed
    }

    val candidate = schemeRemoved.substringBefore("/")
    val parts = candidate.split(":")
    if (parts.size != 2) {
        return null
    }

    val hostPart = parts[0].trim()
    val portPart = parts[1].trim()

    if (hostPart.isEmpty() || portPart.isEmpty()) {
        return null
    }

    val portNumber = portPart.toIntOrNull() ?: return null
    if (portNumber !in 1..65535) {
        return null
    }

    val hostRegex = Regex("^[A-Za-z0-9.-]+$")
    if (!hostRegex.matches(hostPart)) {
        return null
    }

    return "$hostPart:$portNumber"
}
