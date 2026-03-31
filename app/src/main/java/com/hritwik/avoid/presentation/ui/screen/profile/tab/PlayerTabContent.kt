package com.hritwik.avoid.presentation.ui.screen.profile.tab

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hevc
import androidx.compose.material.icons.filled.HdrOn
import androidx.compose.material.icons.filled.Monitor
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideoSettings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.R
import com.hritwik.avoid.presentation.ui.components.common.SettingItem
import com.hritwik.avoid.presentation.ui.components.common.SettingItemWithSwitch
import com.hritwik.avoid.presentation.ui.components.dialogs.DecoderSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DisplayModeSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.HdrFormatSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.MpvConfigDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PlayerProgressColorDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PlayerSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PreferredAudioCodecDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PreferredVideoCodecDialog
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.ui.theme.resolvePlayerProgressColorLabel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun PlayerTabContent(
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val settings by userDataViewModel.personalizationSettings.collectAsStateWithLifecycle()
    val displayMode = playbackSettings.displayMode
    val decoderMode = playbackSettings.decoderMode
    val hdrFormatPreference = playbackSettings.hdrFormatPreference
    val playerType = playbackSettings.playerType
    val preferredVideoCodec = playbackSettings.preferredVideoCodec
    val preferredAudioCodec = playbackSettings.preferredAudioCodec
    val mpvConfig by userDataViewModel.mpvConfig.collectAsStateWithLifecycle()
    val gestures = settings.gesturesEnabled
    val progressBarColorKey by userDataViewModel.playerProgressColor.collectAsStateWithLifecycle()
    val seekColorKey by userDataViewModel.playerProgressSeekColor.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(calculateRoundedValue(16).sdp),
        verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
    ) {
        item {
            Text(
                text = "Player Setting",
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
            var showMpvConfigDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.Code,
                title = "MPV Config",
                subtitle = "Customize mpv.conf for advanced playback",
                onClick = {
                    userDataViewModel.refreshMpvConfig()
                    showMpvConfigDialog = true
                },
                trailingText = "Edit"
            )

            if (showMpvConfigDialog) {
                MpvConfigDialog(
                    initialValue = mpvConfig,
                    onDismiss = { showMpvConfigDialog = false },
                    onSave = { newConfig ->
                        userDataViewModel.saveMpvConfig(newConfig)
                        showMpvConfigDialog = false
                    }
                )
            }
        }

        item {
            var showVideoCodecDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.Hevc,
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
                icon = Icons.Filled.GraphicEq,
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
            var showHdrDialog by remember { mutableStateOf(false) }

            SettingItem(
                icon = Icons.Default.HdrOn,
                title = "HDR Format",
                subtitle = "Choose HDR10+ or Dolby Vision",
                onClick = { showHdrDialog = true },
                trailingText = hdrFormatPreference.displayName
            )

            if (showHdrDialog) {
                HdrFormatSelectionDialog(
                    currentPreference = hdrFormatPreference,
                    onPreferenceSelected = { preference ->
                        userDataViewModel.setHdrFormatPreference(preference)
                        showHdrDialog = false
                    },
                    onDismiss = { showHdrDialog = false }
                )
            }
        }

        item {
            var showProgressDialog by remember { mutableStateOf(false) }
            val progressColorLabel = resolvePlayerProgressColorLabel(progressBarColorKey)

            SettingItem(
                icon = Icons.Default.GraphicEq,
                title = "Seekbar Color",
                subtitle = "Customize playback progress colors",
                onClick = { showProgressDialog = true },
                trailingText = progressColorLabel
            )

            if (showProgressDialog) {
                PlayerProgressColorDialog(
                    currentColorKey = progressBarColorKey,
                    currentSeekColorKey = seekColorKey,
                    onColorSaved = { colorKey, seekKey ->
                        userDataViewModel.setPlayerProgressColor(colorKey)
                        userDataViewModel.setPlayerProgressSeekColor(seekKey)
                        showProgressDialog = false
                    },
                    onDismiss = { showProgressDialog = false }
                )
            }
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.TouchApp,
                title = "Gesture Controls",
                subtitle = "Enable player gestures",
                checked = gestures,
                onCheckedChange = { userDataViewModel.setGesturesEnabled(it) }
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(calculateRoundedValue(130).sdp)
            )
        }
    }
}
