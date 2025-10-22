package com.hritwik.avoid.presentation.ui.screen.profile.tab

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.presentation.ui.components.common.SettingItem
import com.hritwik.avoid.presentation.ui.components.common.SettingItemWithSwitch
import com.hritwik.avoid.presentation.ui.components.dialogs.DownloadCodecDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DownloadQualityDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.StorageLocationDialog
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun DownloadTabContent(
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val downloadSettings by userDataViewModel.downloadSettings.collectAsStateWithLifecycle()
    val wifiOnly = downloadSettings.downloadWifiOnly
    val downloadQuality = downloadSettings.downloadQuality
    val downloadCodec = downloadSettings.downloadCodec
    val storageLocation = downloadSettings.storageLocation

    var showQualityDialog by remember { mutableStateOf(false) }
    var showStorageDialog by remember { mutableStateOf(false) }
    var showCodecDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        
        item {
            Text(
                text = "Download Setting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Storage,
                title = "Storage Location",
                subtitle = "Manage download storage",
                trailingText = if (storageLocation == "internal") "Internal" else "External",
                onClick = {
                    showStorageDialog = true
                }
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Tune,
                title = "Preferred Codec",
                subtitle = "Choose video codec for downloads",
                trailingText = downloadCodec.label,
                onClick = { showCodecDialog = true }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.Wifi,
                title = "Download via WiFi Only",
                subtitle = "Only download when connected to WiFi",
                checked = wifiOnly,
                onCheckedChange = { userDataViewModel.setDownloadWifiOnly(it) }
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Refresh,
                title = "Clear Cache",
                subtitle = "Remove cached data",
                onClick = {
                    userDataViewModel.clearCache()
                    Toast.makeText(context, "Cache Deleted", Toast.LENGTH_SHORT).show()
                },
                destructive = true
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Download,
                title = "Clear Downloads",
                subtitle = "Remove all downloaded content",
                onClick = {
                    userDataViewModel.clearDownloads()
                    Toast.makeText(context, "All downloads deleted", Toast.LENGTH_SHORT).show()
                },
                destructive = true
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(calculateRoundedValue(130).sdp)
            )
        }
    }

    if (showQualityDialog) {
        DownloadQualityDialog(
            options = userDataViewModel.availableDownloadQualities,
            currentQuality = downloadQuality,
            onQualitySelected = {
                userDataViewModel.setDownloadQuality(it)
                showQualityDialog = false
            },
            onDismiss = { showQualityDialog = false }
        )
    }

    if (showCodecDialog) {
        DownloadCodecDialog(
            options = userDataViewModel.availableDownloadCodecs,
            currentCodec = downloadCodec,
            onCodecSelected = {
                userDataViewModel.setDownloadCodec(it)
                showCodecDialog = false
            },
            onDismiss = { showCodecDialog = false }
        )
    }

    if (showStorageDialog) {
        StorageLocationDialog(
            currentLocation = storageLocation,
            onLocationSelected = {
                userDataViewModel.setStorageLocation(it)
                showStorageDialog = false
            },
            onDismiss = { showStorageDialog = false }
        )
    }
}