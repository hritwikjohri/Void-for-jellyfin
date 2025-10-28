package com.hritwik.avoid.presentation.ui.screen.profile.tab

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hritwik.avoid.presentation.ui.components.common.SettingItem
import com.hritwik.avoid.presentation.ui.components.common.SettingItemWithSwitch
import com.hritwik.avoid.presentation.ui.theme.PrimaryText
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp

@Composable
fun VoidTabContent(
    onNavigateToChangePassword: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToTeamVoid: () -> Unit,
    onTvLogin: () -> Unit,
    isOffline: Boolean,
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val homeSettings by userDataViewModel.homeSettings.collectAsStateWithLifecycle()
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val showFeaturedHeader = homeSettings.showFeaturedHeader
    val ambientBackgroundEnabled = homeSettings.ambientBackground
    val navigateEpisodesToSeason = homeSettings.navigateEpisodesToSeason
    val themeSongsEnabled = playbackSettings.playThemeSongs
    val autoPlayNext = playbackSettings.autoPlayNextEpisode
    val autoSkip = playbackSettings.autoSkipSegments
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
                text = "Quick Action",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Info,
                title = "About Void",
                subtitle = "Learn more about Void and it's team",
                onClick = onNavigateToTeamVoid
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
            SettingItem(
                icon = Icons.Default.Favorite,
                title = "My Favorites",
                subtitle = "View your favorite movies and shows",
                onClick = onNavigateToFavorites
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Download,
                title = "Downloads",
                subtitle = "Manage offline content",
                onClick = onNavigateToDownloads
            )
        }

        item {
            SettingItem(
                icon = Icons.Default.Tv,
                title = "Void TV Login",
                subtitle = if (isOffline) "Available when online" else "Quick login to Void TV",
                onClick = {
                    if (serverActionsEnabled) {
                        onTvLogin()
                    } else {
                        Toast.makeText(context, offlineMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        item {
            Text(
                text = "Void Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryText,
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.Star,
                title = "Featured Header",
                subtitle = "Show featured items on home",
                checked = showFeaturedHeader,
                onCheckedChange = { userDataViewModel.setShowFeaturedHeader(it) }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.BlurOn,
                title = "Animated Background",
                subtitle = "Enable animated ambient backgrounds",
                checked = ambientBackgroundEnabled,
                onCheckedChange = { userDataViewModel.setAmbientBackgroundEnabled(it) }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.Tv,
                title = "Open Season from Episodes",
                subtitle = "Tap episodes on home to open their season",
                checked = navigateEpisodesToSeason,
                onCheckedChange = { userDataViewModel.setNavigateEpisodesToSeason(it) }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.MusicNote,
                title = "Play Theme Songs",
                subtitle = "Automatically play theme music on details",
                checked = themeSongsEnabled,
                onCheckedChange = { userDataViewModel.setPlayThemeSongs(it) }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.PlayArrow,
                title = "Auto play next episode",
                subtitle = "Automatically play the next episode",
                checked = autoPlayNext,
                onCheckedChange = { enabled ->
                    userDataViewModel.setAutoPlay(enabled)
                }
            )
        }

        item {
            SettingItemWithSwitch(
                icon = Icons.Default.SkipNext,
                title = "Auto Skip Intros/Credits",
                subtitle = "Automatically skip Intro/Outro/Credits",
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
}