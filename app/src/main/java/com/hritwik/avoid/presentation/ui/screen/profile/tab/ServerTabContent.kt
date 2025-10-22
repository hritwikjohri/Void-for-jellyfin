package com.hritwik.avoid.presentation.ui.screen.profile.tab

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.hritwik.avoid.domain.model.auth.ServerConnectionMethod
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.presentation.ui.components.common.ServerInfoRow
import com.hritwik.avoid.presentation.ui.components.common.SettingItem
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun ServerTabContent(
    userData: UserData,
    connectionMethods: List<ServerConnectionMethod>,
    activeConnection: ServerConnectionMethod?,
    isOffline: Boolean,
    isWifiConnected: Boolean,
    onSwitchUser: () -> Unit,
    onLogoutClick: () -> Unit,
    onConnectionDashboard: () -> Unit
) {
    val serverActionsEnabled = !isOffline
    val offlineMessage = "Reconnect to manage server settings"
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        item {
            Text(
                text = "Server Info",
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
                    modifier = Modifier.padding(calculateRoundedValue(16).sdp)
                ) {
                    Text(
                        text = "Server Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(calculateRoundedValue(8).sdp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(4).sdp)
                    ) {
                        ServerInfoRow("Status", if (isOffline) "Offline" else "Online")
                        ServerInfoRow("Wi-Fi", if (isWifiConnected) "Connected" else "Not connected")
                        ServerInfoRow("Server Name", userData.serverName ?: "")
                        ServerInfoRow("Preferred URL", userData.serverUrl ?: "")
                        activeConnection?.let { method ->
                            val typeLabel = if (method.isLocal) "Local" else "Internet"
                            ServerInfoRow("Active Connection", method.url)
                            ServerInfoRow("Connection Type", typeLabel)
                        }
                        if (connectionMethods.isNotEmpty()) {
                            connectionMethods.forEachIndexed { index, method ->
                                val label = "Priority ${index + 1}"
                                val connectionLabel = if (method.isLocal) "Local" else "Internet"
                                val value = buildString {
                                    append(method.url)
                                    append(" ($connectionLabel)")
                                    if (method == activeConnection) {
                                        append(" - active")
                                    }
                                }
                                ServerInfoRow(label, value)
                            }
                        }
                        ServerInfoRow("User ID", userData.email ?: "", mask = false)
                    }
                }
            }
        }

        item {
            Text(
                text = "Server Setting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Dashboard,
                title = "Connection Dashboard",
                subtitle = if (isOffline) "Available when online" else "View running services",
                onClick = {
                    if (serverActionsEnabled) {
                        onConnectionDashboard()
                    } else {
                        Toast.makeText(context, offlineMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Person,
                title = "Switch User",
                subtitle = if (isOffline) "Reconnect to change users" else "Login with another account on this server",
                onClick = {
                    if (serverActionsEnabled) {
                        onSwitchUser()
                    } else {
                        Toast.makeText(context, offlineMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item {
            SettingItem(
                icon = Icons.AutoMirrored.Outlined.Logout,
                title = "Sign Out",
                subtitle = "Sign out from this device",
                onClick = { onLogoutClick() },
                destructive = true
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(calculateRoundedValue(130).sdp)
            )
        }
    }
}
