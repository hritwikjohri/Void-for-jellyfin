package com.hritwik.avoid.presentation.ui.screen.profile.connectionDashboard

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.hritwik.avoid.data.connection.ServerConnectionEvent
import com.hritwik.avoid.presentation.ui.components.common.ScreenHeader
import com.hritwik.avoid.presentation.ui.components.common.SubtleShinySignature
import com.hritwik.avoid.presentation.ui.components.visual.AnimatedAmbientBackground
import com.hritwik.avoid.utils.extensions.clearFocusOnTap
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ConnectionDashboardScreen(
    localConnections: List<String>,
    remoteConnections: List<String>,
    connectionEvents: Flow<ServerConnectionEvent>,
    onLocalConnectionsSaved: (List<String>) -> Unit,
    onClearLocalConnections: () -> Unit,
    onRemoteConnectionsSaved: (List<String>) -> Unit,
    onClearRemoteConnections: () -> Unit,
    onRefreshConnection: () -> Unit,
    onBackClick: () -> Unit
) {
    var localInputs by remember { mutableStateOf(localConnections.ifEmpty { listOf("") }) }
    var remoteInputs by remember { mutableStateOf(remoteConnections.ifEmpty { listOf("") }) }
    var isRefreshing by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(localConnections) {
        val sanitized = localConnections
        val currentSanitized = localInputs.map { it.trim() }.filter { it.isNotEmpty() }
        if (sanitized != currentSanitized) {
            localInputs = sanitized.ifEmpty { listOf("") }
        }
    }

    LaunchedEffect(remoteConnections) {
        val sanitized = remoteConnections
        val currentSanitized = remoteInputs.map { it.trim() }.filter { it.isNotEmpty() }
        if (sanitized != currentSanitized) {
            remoteInputs = sanitized.ifEmpty { listOf("") }
        }
    }

    LaunchedEffect(connectionEvents) {
        connectionEvents.collectLatest { event ->
            isRefreshing = false
            val message = when (event) {
                is ServerConnectionEvent.MethodSwitched -> event.message
                is ServerConnectionEvent.Offline -> event.message
            }
            if (message.isNotBlank()) {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun moveLocal(from: Int, to: Int) {
        if (from == to) return
        localInputs = localInputs.toMutableList().also { list ->
            val item = list.removeAt(from)
            list.add(to, item)
        }
    }

    fun removeLocal(index: Int) {
        val updated = localInputs.toMutableList().also { it.removeAt(index) }
        localInputs = if (updated.isEmpty()) listOf("") else updated
    }

    fun moveRemote(from: Int, to: Int) {
        if (from == to) return
        remoteInputs = remoteInputs.toMutableList().also { list ->
            val item = list.removeAt(from)
            list.add(to, item)
        }
    }

    fun removeRemote(index: Int) {
        val updated = remoteInputs.toMutableList().also { it.removeAt(index) }
        remoteInputs = if (updated.isEmpty()) listOf("") else updated
    }

    val sanitizedLocal = remember(localInputs) {
        localInputs.map { it.trim() }.filter { it.isNotEmpty() }
    }
    val sanitizedRemote = remember(remoteInputs) {
        remoteInputs.map { it.trim() }.filter { it.isNotEmpty() }
    }

    AnimatedAmbientBackground {
        Column (
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.ime)
                .imePadding()
                .clearFocusOnTap()
        ) {
            ScreenHeader(
                title = "Connection Dashboard",
                onBackClick = onBackClick,
                showBackButton = true
            )

            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .imePadding()
                    .padding(horizontal = calculateRoundedValue(16).sdp)
                    .padding(bottom = calculateRoundedValue(16).sdp)
            ) {
                ConnectionStatusCard(
                    isRefreshing = isRefreshing,
                    onRefreshClick = {
                        isRefreshing = true
                        onRefreshConnection()
                    }
                )

                Spacer(modifier = Modifier.height(calculateRoundedValue(20).sdp))

                ConnectionSection(
                    title = "Local Network",
                    subtitle = "Add local IPs or hostnames for Wi-Fi connectivity",
                    icon = Icons.Default.Wifi,
                    inputs = localInputs,
                    onInputChange = { index, value ->
                        localInputs = localInputs.toMutableList().also { it[index] = value }
                    },
                    onMove = ::moveLocal,
                    onRemove = ::removeLocal,
                    onAdd = { localInputs = localInputs + "" },
                    onSave = { onLocalConnectionsSaved(sanitizedLocal) },
                    onClear = {
                        localInputs = listOf("")
                        onClearLocalConnections()
                    },
                    sanitizedInputs = sanitizedLocal,
                    savedConnections = localConnections,
                    labelPrefix = "Local address"
                )

                Spacer(modifier = Modifier.height(calculateRoundedValue(20).sdp))

                ConnectionSection(
                    title = "Internet",
                    subtitle = "Add public URLs for remote access",
                    icon = Icons.Outlined.Cloud,
                    inputs = remoteInputs,
                    onInputChange = { index, value ->
                        remoteInputs = remoteInputs.toMutableList().also { it[index] = value }
                    },
                    onMove = ::moveRemote,
                    onRemove = ::removeRemote,
                    onAdd = { remoteInputs = remoteInputs + "" },
                    onSave = { onRemoteConnectionsSaved(sanitizedRemote) },
                    onClear = {
                        remoteInputs = listOf("")
                        onClearRemoteConnections()
                    },
                    sanitizedInputs = sanitizedRemote,
                    savedConnections = remoteConnections,
                    labelPrefix = "Internet address"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            SubtleShinySignature(
                modifier = Modifier.padding(bottom = calculateRoundedValue(90).sdp)
            )
        }
    }
}