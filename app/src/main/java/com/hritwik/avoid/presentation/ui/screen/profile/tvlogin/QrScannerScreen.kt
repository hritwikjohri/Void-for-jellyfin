package com.hritwik.avoid.presentation.ui.screen.profile.tvlogin

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.scanner.QrScanner

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QrScannerScreen(
    onBack: () -> Unit,
    onQrCodeDetected: (String) -> Unit,
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var torchEnabled by remember { mutableStateOf(false) }
    var torchAvailable by remember { mutableStateOf(false) }
    var scanConsumed by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val status = cameraPermissionState.status) {
            PermissionStatus.Granted -> {
                QrScanner(
                    modifier = Modifier.fillMaxSize(),
                    torchEnabled = torchEnabled,
                    onTorchAvailabilityChanged = { available ->
                        torchAvailable = available
                        if (!available) {
                            torchEnabled = false
                        }
                    },
                    onTorchStateChanged = { enabled -> torchEnabled = enabled },
                    onQrCodeScanned = { value ->
                        if (!scanConsumed) {
                            scanConsumed = true
                            torchEnabled = false
                            onQrCodeDetected(value)
                        }
                    }
                )

                ScannerOverlay(
                    modifier = Modifier.fillMaxSize(),
                    onBack = onBack,
                    torchEnabled = torchEnabled,
                    torchAvailable = torchAvailable,
                    onToggleTorch = { torchEnabled = !torchEnabled }
                )
            }

            is PermissionStatus.Denied -> {
                PermissionExplanation(
                    modifier = Modifier.align(Alignment.Center),
                    shouldShowRationale = status.shouldShowRationale,
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
                    onBack = onBack
                )
            }
        }
    }
}

@Composable
private fun ScannerOverlay(
    modifier: Modifier,
    onBack: () -> Unit,
    torchEnabled: Boolean,
    torchAvailable: Boolean,
    onToggleTorch: () -> Unit,
) {
    Box(modifier = modifier.background(color = Color.Black.copy(alpha = 0.4f))) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(vertical = 46.dp, horizontal = 16.dp)
                .align(Alignment.TopEnd)
                .size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = null,
                tint = Color.White
            )
        }

        AsyncImage(
            model = R.drawable.void_icon,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(0.4f).align(Alignment.TopCenter)
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Align the QR code within the frame",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "We'll automatically capture the first valid code",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            if (torchAvailable) {
                Button(
                    onClick = onToggleTorch,
                    modifier = Modifier
                        .padding(top = 8.dp)
                ) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Filled.FlashlightOff else Icons.Filled.FlashlightOn,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = if (torchEnabled) "Turn off flashlight" else "Turn on flashlight")
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun PermissionExplanation(
    modifier: Modifier,
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = modifier
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.FlashlightOff,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(72.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .padding(16.dp)
        )

        Text(
            text = if (shouldShowRationale) {
                "Camera access is required to scan QR codes."
            } else {
                "We need camera permission to start the scanner."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )

        Button(onClick = onRequestPermission) {
            Text(text = "Allow camera access")
        }

        TextButton(onClick = onBack) {
            Text(text = "Cancel")
        }
    }
}
