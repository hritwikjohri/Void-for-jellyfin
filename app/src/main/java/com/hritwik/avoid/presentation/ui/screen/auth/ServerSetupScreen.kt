package com.hritwik.avoid.presentation.ui.screen.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.common.SubtleShinySignature
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.utils.extensions.clearFocusOnTap
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupScreen(
    onServerConnected: () -> Unit,
    viewModel: AuthServerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var serverUrl by remember { mutableStateOf(state.serverUrl ?: "") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    var mtlsPasswordVisible by remember { mutableStateOf(false) }
    val certificatePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.clearMtlsError()
            viewModel.importMtlsCertificate(context, it)
            viewModel.setMtlsEnabled(true)
        }
    }
    val canConnect = !state.isLoading &&
            serverUrl.isNotBlank() &&
            !state.isMtlsImporting &&
            (!state.isMtlsEnabled || state.mtlsCertificateName != null)
    val certificateLabel = state.mtlsCertificateName?.let { name ->
        "Selected certificate: $name"
    } ?: if (state.isMtlsEnabled) "No certificate selected" else "mTLS off (upload to auto-enable)"

    LaunchedEffect(state.mtlsCertificateName) {
        mtlsPasswordVisible = false
    }

    LaunchedEffect(state.serverUrl) {
        if (state.serverUrl != null && serverUrl.isEmpty()) {
            serverUrl = state.serverUrl ?: ""
        }
    }

    LaunchedEffect(Unit) {
        viewModel.resetConnectionState()
    }

    LaunchedEffect(state.isConnected, state.server) {
        if (state.isConnected && state.server != null) {
            onServerConnected()
        }
    }

    AnimatedAmbientBackground(
        drawableRes = R.drawable.jellyfin_logo,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .imePadding()
                .clearFocusOnTap()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(calculateRoundedValue(16).sdp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.weight(0.7f))

                AsyncImage(
                    model = R.drawable.void_icon,
                    contentDescription = null,
                    modifier = Modifier.size(calculateRoundedValue(180).sdp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))

                Text(
                    text = "Connect to Jellyfin",
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                Text(
                    text = "Enter your Jellyfin server URL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(calculateRoundedValue(32).sdp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        if (state.error != null) {
                            viewModel.clearError()
                        }
                    },
                    label = { Text("Server URL") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            if (serverUrl.isNotBlank() && !state.isLoading && !state.isMtlsImporting) {
                                viewModel.connectToServer(serverUrl)
                            }
                        }
                    ),
                    shape = RoundedCornerShape(calculateRoundedValue(28).sdp),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && !state.isMtlsImporting,
                    singleLine = true,
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

                Button(
                    onClick = {
                        keyboardController?.hide()
                        if (serverUrl.isNotBlank() && !state.isLoading && !state.isMtlsImporting) {
                            viewModel.connectToServer(serverUrl)
                        }
                    },
                    enabled = canConnect,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (state.isLoading) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(calculateRoundedValue(20).sdp),
                                strokeWidth = calculateRoundedValue(2).sdp
                            )
                            Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                            Text("Connecting…")
                        }
                    } else {
                        Text(if (state.isMtlsEnabled) "Connect using mTLS" else "Connect")
                    }
                }

                Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                Text(
                    text = certificateLabel,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.mtlsCertificateName != null) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (state.isMtlsEnabled && state.mtlsCertificateName != null) {
                    Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                    OutlinedTextField(
                        value = state.mtlsCertificatePassword,
                        onValueChange = { viewModel.updateMtlsCertificatePassword(it) },
                        label = { Text("Certificate password (optional)") },
                        visualTransformation = if (mtlsPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        trailingIcon = {
                            IconButton(onClick = { mtlsPasswordVisible = !mtlsPasswordVisible }) {
                                Icon(
                                    imageVector = if (mtlsPasswordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (mtlsPasswordVisible) {
                                        "Hide certificate password"
                                    } else {
                                        "Show certificate password"
                                    },
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        },
                        shape = RoundedCornerShape(calculateRoundedValue(28).sdp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isMtlsImporting && !state.isLoading,
                        singleLine = true,
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
                }

                Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                val hasCertificate = state.mtlsCertificateName != null
                OutlinedButton(
                    onClick = {
                        viewModel.clearMtlsError()
                        if (hasCertificate) {
                            viewModel.removeMtlsCertificate()
                        } else {
                            certificatePickerLauncher.launch(
                                arrayOf(
                                    "application/x-pkcs12",
                                    "application/x-pem-file",
                                    "application/x-pkcs7-certificates",
                                    "*/*"
                                )
                            )
                        }
                    },
                    enabled = !state.isMtlsImporting && !state.isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = Color.White
                    ),
                    border = BorderStroke(0.1.dp, Color.White)
                ) {
                    if (state.isMtlsImporting) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(calculateRoundedValue(18).sdp),
                                strokeWidth = calculateRoundedValue(2).sdp
                            )
                            Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                            Text(if (hasCertificate) "Removing…" else "Importing…")
                        }
                    } else {
                        Text(if (hasCertificate) "Remove certificate" else "Upload certificate")
                    }
                }

                Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                state.mtlsError?.let { error ->
                    Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                state.error?.let { error ->
                    Spacer(modifier = Modifier.height(calculateRoundedValue(16).sdp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.7f))
            }
        }
        SubtleShinySignature(
            modifier = Modifier.padding(bottom = calculateRoundedValue(90).sdp)
        )
    }
}