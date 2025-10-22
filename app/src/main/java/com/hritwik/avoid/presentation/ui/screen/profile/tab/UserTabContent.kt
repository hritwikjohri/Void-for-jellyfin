package com.hritwik.avoid.presentation.ui.screen.profile.tab

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.common.EraseDataDialog
import com.hritwik.avoid.presentation.ui.components.common.SettingItem
import com.hritwik.avoid.presentation.ui.components.common.SettingItemWithSwitch
import com.hritwik.avoid.presentation.ui.components.dialogs.DecoderSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DisplayModeSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PlayerSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PreferredAudioCodecDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PreferredVideoCodecDialog
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun UserTabContent(
    onNavigateToChangePassword: () -> Unit,
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    var showEraseDialog by remember { mutableStateOf(false) }
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val displayMode = playbackSettings.displayMode
    val decoderMode = playbackSettings.decoderMode
    val playerType = playbackSettings.playerType
    val preferredVideoCodec = playbackSettings.preferredVideoCodec
    val preferredAudioCodec = playbackSettings.preferredAudioCodec
    val autoSkip = playbackSettings.autoSkipSegments

    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        
        item {
            Text(
                text = "Account Management",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Lock,
                title = "Change Password",
                subtitle = "Update your account password",
                onClick = onNavigateToChangePassword
            )
        }

        item {
            Text(
                text = "Playback Setting",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            var showPlayerDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.SmartDisplay,
                title = stringResource(id = R.string.setting_player_type_title),
                subtitle = stringResource(id = R.string.setting_player_type_subtitle),
                onClick = { showPlayerDialog = true },
                trailingText = playerType.value
            )

            if (showPlayerDialog) {
                PlayerSelectionDialog(
                    currentPlayer = playerType,
                    onPlayerSelected = { selectedType ->
                        userDataViewModel.setPlayerType(selectedType)
                        showPlayerDialog = false
                    },
                    onDismiss = { showPlayerDialog = false }
                )
            }
        }

        item {
            var showDecoderDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.VideoSettings,
                title = "Video Decoder",
                subtitle = "Choose decoding method for best performance",
                onClick = { showDecoderDialog = true },
                trailingText = decoderMode.value
            )

            if (showDecoderDialog) {
                DecoderSelectionDialog(
                    currentMode = decoderMode,
                    onModeSelected = { mode ->
                        userDataViewModel.setDecoderMode(mode)
                        showDecoderDialog = false
                    },
                    onDismiss = { showDecoderDialog = false }
                )
            }
        }

        item {
            var showVideoCodecDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.Tune,
                title = stringResource(id = R.string.settings_preferred_video_codec_title),
                subtitle = stringResource(id = R.string.settings_preferred_video_codec_subtitle),
                onClick = { showVideoCodecDialog = true },
                trailingText = preferredVideoCodec.label
            )

            if (showVideoCodecDialog) {
                PreferredVideoCodecDialog(
                    selectedCodec = preferredVideoCodec,
                    onCodecSelected = { codec ->
                        userDataViewModel.setPreferredVideoCodec(codec)
                        showVideoCodecDialog = false
                    },
                    onDismiss = { showVideoCodecDialog = false }
                )
            }
        }

        item {
            var showAudioCodecDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                title = stringResource(id = R.string.settings_preferred_audio_codec_title),
                subtitle = stringResource(id = R.string.settings_preferred_audio_codec_subtitle),
                onClick = { showAudioCodecDialog = true },
                trailingText = preferredAudioCodec.label
            )

            if (showAudioCodecDialog) {
                PreferredAudioCodecDialog(
                    selectedCodec = preferredAudioCodec,
                    onCodecSelected = { codec ->
                        userDataViewModel.setPreferredAudioCodec(codec)
                        showAudioCodecDialog = false
                    },
                    onDismiss = { showAudioCodecDialog = false }
                )
            }
        }

        item {
            var showDisplayModeDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.Monitor,
                title = "Display Mode",
                subtitle = "Fit screen, Crop, Stretch, Original",
                onClick = { showDisplayModeDialog = true },
                trailingText = displayMode.value
            )

            if (showDisplayModeDialog) {
                DisplayModeSelectionDialog(
                    currentMode = displayMode,
                    onModeSelected = { mode ->
                        userDataViewModel.setDisplayMode(mode)
                        showDisplayModeDialog = false
                    },
                    onDismiss = { showDisplayModeDialog = false }
                )
            }
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.SkipNext,
                title = "Auto Skip Intros/Credits",
                subtitle = "Automatically skip opening and ending segments",
                checked = autoSkip,
                onCheckedChange = {
                    userDataViewModel.setAutoSkipSegments(it)
                }
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(calculateRoundedValue(130).sdp)
            )
        }
    }

    if (showEraseDialog) {
        EraseDataDialog(
            onConfirm = {
                userDataViewModel.erasePersonalData { success ->
                    Toast.makeText(
                        context,
                        if (success) "Data erased" else "Erase failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                showEraseDialog = false
            },
            onDismiss = { showEraseDialog = false }
        )
    }
}