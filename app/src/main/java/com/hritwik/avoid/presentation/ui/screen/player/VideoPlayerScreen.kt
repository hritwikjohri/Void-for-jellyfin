package com.hritwik.avoid.presentation.ui.screen.player

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel

@SuppressLint("SourceLockedOrientationActivity")
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    mediaItem: MediaItem,
    mediaSourceId: String? = null,
    audioStreamIndex: Int? = null,
    subtitleStreamIndex: Int? = null,
    startPositionMs: Long = 0,
    onBackClick: () -> Unit = {},
    viewModel: VideoPlaybackViewModel = hiltViewModel(),
    authViewModel: AuthServerViewModel = hiltViewModel(),
    userDataViewModel: UserDataViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val playerState by viewModel.state.collectAsStateWithLifecycle()
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val decoderMode = playbackSettings.decoderMode
    val displayMode = playbackSettings.displayMode
    val playerType = playbackSettings.playerType
    val autoSkipSegments = playbackSettings.autoSkipSegments
    val personalization by userDataViewModel.personalizationSettings.collectAsStateWithLifecycle()
    val gesturesEnabled = personalization.gesturesEnabled
    val userId = authState.authSession?.userId?.id ?: ""
    val accessToken = authState.authSession?.accessToken ?: ""
    val serverUrl = authState.authSession?.server?.url ?: ""
    val activity = LocalActivity.current

    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    LaunchedEffect(mediaItem.id, userId, accessToken, serverUrl) {
        viewModel.initializePlayer(
            mediaItem = mediaItem,
            serverUrl = serverUrl,
            accessToken = accessToken,
            userId = userId,
            mediaSourceId = mediaSourceId,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex,
            startPositionMs = startPositionMs
        )
    }

    val preferredPlayerType = playerType

    var resolvedPlayerType by rememberSaveable(mediaItem.id) { mutableStateOf(preferredPlayerType) }

    LaunchedEffect(preferredPlayerType, mediaItem.id) {
        resolvedPlayerType = preferredPlayerType
    }

    val canUseMpv = playerState.videoUrl != null
    val canUseExo = playerState.exoMediaItem != null || playerState.videoUrl != null

    LaunchedEffect(preferredPlayerType, canUseMpv, canUseExo, playerState.isInitialized) {
        if (!playerState.isInitialized) return@LaunchedEffect
        resolvedPlayerType = when (preferredPlayerType) {
            PlayerType.MPV -> if (canUseMpv) {
                PlayerType.MPV
            } else if (canUseExo) {
                PlayerType.EXOPLAYER
            } else {
                PlayerType.MPV
            }

            PlayerType.EXOPLAYER -> if (canUseExo) {
                PlayerType.EXOPLAYER
            } else if (canUseMpv) {
                PlayerType.MPV
            } else {
                PlayerType.EXOPLAYER
            }
        }
    }

    if (playerState.isInitialized) {
        when (resolvedPlayerType) {
            PlayerType.MPV -> MpvPlayerView(
                mediaItem = mediaItem,
                playerState = playerState,
                decoderMode = decoderMode,
                displayMode = displayMode,
                userId = userId,
                accessToken = accessToken,
                serverUrl = serverUrl,
                autoSkipSegments = autoSkipSegments,
                gesturesEnabled = gesturesEnabled,
                onBackClick = onBackClick,
                videoPlaybackViewModel = viewModel,
                userDataViewModel = userDataViewModel
            )

            PlayerType.EXOPLAYER -> ExoPlayerView(
                mediaItem = mediaItem,
                decoderMode = decoderMode,
                displayMode = displayMode,
                userId = userId,
                accessToken = accessToken,
                serverUrl = serverUrl,
                autoSkipSegments = autoSkipSegments,
                gesturesEnabled = gesturesEnabled,
                onBackClick = onBackClick,
                viewModel = viewModel,
                userDataViewModel = userDataViewModel
            )
        }
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Preparing video...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}