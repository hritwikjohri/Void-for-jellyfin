package com.hritwik.avoid.presentation.ui.screen.profile.tab

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.ui.graphics.Color
import com.hritwik.avoid.domain.model.jellyseer.JellyseerConfig
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.delay

@Composable
fun JellyseerTabContent(
    config: JellyseerConfig,
    authState: UserDataViewModel.JellyseerAuthUiState,
    onBaseUrlChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onLogin: (String, String, String) -> Unit,
    onLogout: () -> Unit,
    onClearFeedback: () -> Unit,
) {
    val context = LocalContext.current

    var baseUrl by remember { mutableStateOf(config.baseUrl) }
    var username by remember { mutableStateOf(config.userDisplayName.orEmpty()) }
    var password by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf(config.apiKey) }
    var showPassword by remember { mutableStateOf(false) }

    LaunchedEffect(config.baseUrl) {
        if (config.baseUrl != baseUrl) {
            baseUrl = config.baseUrl
        }
    }

    LaunchedEffect(config.apiKey) {
        if (config.apiKey != apiKey) {
            apiKey = config.apiKey
        }
    }

    LaunchedEffect(config.userDisplayName) {
        if (!config.userDisplayName.isNullOrBlank() && username.isBlank()) {
            username = config.userDisplayName
        }
    }

    LaunchedEffect(authState.successMessage) {
        if (!authState.successMessage.isNullOrBlank()) {
            Toast.makeText(context, authState.successMessage, Toast.LENGTH_SHORT).show()
            password = ""
        }
    }

    LaunchedEffect(authState.errorMessage) {
        if (!authState.errorMessage.isNullOrBlank()) {
            Toast.makeText(context, authState.errorMessage, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(authState.errorMessage, authState.successMessage) {
        if (!authState.errorMessage.isNullOrBlank() || !authState.successMessage.isNullOrBlank()) {
            delay(2_500)
            onClearFeedback()
        }
    }

    val statusText = when {
        config.isLoggedIn -> buildString {
            append("Signed in as ")
            append(
                config.userDisplayName
                    ?: config.userEmail
                    ?: "current user"
            )
        }

        config.apiKey.isNotBlank() -> "API key configured. Requests will use the API key."

        else -> "Provide a base URL and sign in with your Jellyfin credentials to enable Jellyseer features."
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        item {
            Text(
                text = "Jellyseer Integration",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(0).sdp),
                shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(
                    width = calculateRoundedValue(1).sdp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                        Text(
                            text = "Server",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Enter the Jellyseer server URL (for example, https://requests.example.com).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = baseUrl,
                        onValueChange = {
                            baseUrl = it
                            onBaseUrlChange(it)
                        },
                        label = { Text("Base URL") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Cloud, contentDescription = null)
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (config.isConfigured) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(0).sdp),
                shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(
                    width = calculateRoundedValue(1).sdp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                        Text(
                            text = "Jellyfin Account",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Sign in with your Jellyfin credentials to link your Jellyseer account.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(visible = config.isLoggedIn && !config.userDisplayName.isNullOrBlank()) {
                        Text(
                            text = "Currently signed in as ${config.userDisplayName}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Username") },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        singleLine = true,
                        leadingIcon = { Icon(imageVector = Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onLogin(baseUrl.trim(), username.trim(), password) },
                        enabled = !authState.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(calculateRoundedValue(18).sdp),
                                strokeWidth = calculateRoundedValue(2).sdp
                            )
                        } else {
                            Text(text = if (config.isLoggedIn) "Re-authenticate" else "Sign In")
                        }
                    }

                    if (config.isLoggedIn) {
                        OutlinedButton(
                            onClick = onLogout,
                            enabled = !authState.isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Outlined.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(calculateRoundedValue(8).sdp))
                            Text(text = "Sign Out")
                        }
                    }

                    when {
                        !authState.errorMessage.isNullOrBlank() -> {
                            Text(
                                text = authState.errorMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }

                        !authState.successMessage.isNullOrBlank() -> {
                            Text(
                                text = authState.successMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = calculateRoundedValue(0).sdp),
                shape = RoundedCornerShape(calculateRoundedValue(16).sdp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                ),
                border = BorderStroke(
                    width = calculateRoundedValue(1).sdp,
                    color = Color.White.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(calculateRoundedValue(16).sdp),
                    verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(12).sdp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)) {
                        Text(
                            text = "Fallback API Key",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryText
                        )
                        Text(
                            text = "Optional. Used when not signed in.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            onApiKeyChange(it)
                        },
                        label = { Text("API Key") },
                        leadingIcon = { Icon(imageVector = Icons.Outlined.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(calculateRoundedValue(120).sdp))
        }
    }
}
