package com.hritwik.avoid.presentation.ui.screen.player

import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.hritwik.avoid.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.cast.CastEntryPoint
import com.hritwik.avoid.cast.CastMediaHelper
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.presentation.ui.components.cast.CastingOverlay
import com.hritwik.avoid.presentation.viewmodel.auth.AuthServerViewModel
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import dagger.hilt.android.EntryPointAccessors

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
    val autoPlayNextEpisode = playbackSettings.autoPlayNextEpisode
    val autoSkipSegments = playbackSettings.autoSkipSegments
    val personalization by userDataViewModel.personalizationSettings.collectAsStateWithLifecycle()
    val gesturesEnabled = personalization.gesturesEnabled
    val userId = authState.authSession?.userId?.id ?: ""
    val accessToken = authState.authSession?.accessToken ?: ""
    val serverUrl = authState.authSession?.server?.url ?: ""
    val activity = LocalActivity.current
    val context = LocalContext.current
    val castManager = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            CastEntryPoint::class.java
        ).castManager()
    }
    val isCasting by castManager.isCasting.collectAsStateWithLifecycle()
    val castDeviceName by castManager.castDeviceName.collectAsStateWithLifecycle()

    DisposableEffect(isCasting) {
        activity?.requestedOrientation = if (isCasting) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        }
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
    val selectedVideoStream = playerState.playbackOptions.selectedVideoStream
        ?: mediaItem.getPrimaryMediaSource()?.defaultVideoStream
    val shouldPreferExoForAuto = selectedVideoStream?.isHdrOrDovi() == true

    LaunchedEffect(
        preferredPlayerType,
        canUseMpv,
        canUseExo,
        playerState.isInitialized,
        shouldPreferExoForAuto
    ) {
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

            PlayerType.AUTO -> if (shouldPreferExoForAuto) {
                if (canUseExo) {
                    PlayerType.EXOPLAYER
                } else if (canUseMpv) {
                    PlayerType.MPV
                } else {
                    PlayerType.EXOPLAYER
                }
            } else {
                if (canUseMpv) {
                    PlayerType.MPV
                } else if (canUseExo) {
                    PlayerType.EXOPLAYER
                } else {
                    PlayerType.MPV
                }
            }
        }
    }

    if (isCasting && playerState.isInitialized) {
        val videoUrl = CastMediaHelper.buildCastStreamUrl(
            serverUrl = serverUrl,
            itemId = mediaItem.id,
            mediaSourceId = mediaSourceId ?: playerState.playbackOptions.selectedMediaSource?.id,
            accessToken = accessToken,
            audioStreamIndex = audioStreamIndex ?: playerState.playbackOptions.selectedAudioStream?.index,
            subtitleStreamIndex = subtitleStreamIndex ?: playerState.playbackOptions.selectedSubtitleStream?.index
        )
        LaunchedEffect(mediaItem.id, videoUrl) {
            val castMediaItem = CastMediaHelper.buildCastMediaItem(
                mediaItem = mediaItem,
                videoUrl = videoUrl,
                serverUrl = serverUrl,
                startPositionMs = startPositionMs
            )
            castManager.castPlayer?.setMediaItem(castMediaItem, startPositionMs)
            castManager.castPlayer?.prepare()
            castManager.castPlayer?.play()
        }

        // Duration & progress polling – mirrors the ExoPlayer loop in
        // ExoPlayerView (lines 849-862) using the same dual-source logic:
        //   1. Server-provided override (totalDurationSeconds from the API)
        //   2. Player-reported duration (castPlayer.duration)
        //   3. Jellyfin runTimeTicks as final fallback
        val apiDurationSeconds = playerState.totalDurationSeconds
        val runTimeTicksDurationMs = mediaItem.runTimeTicks?.let { it / 10_000 } ?: 0L
        var castDurationMs by remember { mutableLongStateOf(runTimeTicksDurationMs) }
        var castPositionMs by remember { mutableLongStateOf(startPositionMs) }

        LaunchedEffect(castManager.castPlayer, apiDurationSeconds) {
            val player = castManager.castPlayer ?: return@LaunchedEffect
            while (isActive) {
                // Same priority as ExoPlayer: API override > player > runTimeTicks
                val overrideDurationMs = apiDurationSeconds?.takeIf { it > 0 }?.let { it * 1000 }
                if (overrideDurationMs != null) {
                    castDurationMs = overrideDurationMs
                } else {
                    val playerDurationMs = player.duration
                    if (playerDurationMs > 0) {
                        castDurationMs = playerDurationMs
                    } else if (runTimeTicksDurationMs > 0) {
                        castDurationMs = runTimeTicksDurationMs
                    }
                }
                castPositionMs = player.currentPosition.coerceAtLeast(0L)
                delay(1000)
            }
        }

        CastingOverlay(
            deviceName = castDeviceName,
            mediaTitle = mediaItem.name,
            durationMs = castDurationMs,
            positionMs = castPositionMs,
            onDisconnect = {
                castManager.getCurrentSession()?.remoteMediaClient?.stop()
            }
        )
    } else if (isCasting && !playerState.isInitialized) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(calculateRoundedValue(16).sdp)
            ) {
                AsyncImage(
                    model = R.drawable.void_icon,
                    contentDescription = "Loading",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(calculateRoundedValue(120).sdp)
                )
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Connecting to ${castDeviceName ?: "device"}...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    } else if (playerState.isInitialized) {
        when (resolvedPlayerType) {
            PlayerType.MPV -> MpvPlayerView(
                mediaItem = mediaItem,
                playerState = playerState,
                decoderMode = decoderMode,
                displayMode = displayMode,
                userId = userId,
                accessToken = accessToken,
                serverUrl = serverUrl,
                autoPlayNextEpisode = autoPlayNextEpisode,
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
                autoPlayNextEpisode = autoPlayNextEpisode,
                autoSkipSegments = autoSkipSegments,
                gesturesEnabled = gesturesEnabled,
                onBackClick = onBackClick,
                viewModel = viewModel,
                userDataViewModel = userDataViewModel
            )

            PlayerType.AUTO -> MpvPlayerView(
                mediaItem = mediaItem,
                playerState = playerState,
                decoderMode = decoderMode,
                displayMode = displayMode,
                userId = userId,
                accessToken = accessToken,
                serverUrl = serverUrl,
                autoPlayNextEpisode = autoPlayNextEpisode,
                autoSkipSegments = autoSkipSegments,
                gesturesEnabled = gesturesEnabled,
                onBackClick = onBackClick,
                videoPlaybackViewModel = viewModel,
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

private fun MediaStream.isHdrOrDovi(): Boolean {
    val normalizedType = videoRangeType
        ?.replace("_", "")
        ?.replace("-", "")
        ?.uppercase()

    if (normalizedType != null) {
        if (normalizedType.contains("DOVI")) return true
        if (normalizedType.contains("HDR")) return true
        if (normalizedType.contains("HLG")) return true
    }

    return videoRange?.equals("HDR", ignoreCase = true) == true
}
