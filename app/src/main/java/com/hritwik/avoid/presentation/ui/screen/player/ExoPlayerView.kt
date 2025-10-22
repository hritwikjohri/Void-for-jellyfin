package com.hritwik.avoid.presentation.ui.screen.player

import android.app.PictureInPictureParams
import android.content.Context
import android.graphics.Typeface
import android.media.AudioManager
import android.util.Rational
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.presentation.ui.components.common.rememberAudioFocusRequest
import com.hritwik.avoid.presentation.ui.components.dialogs.AudioTrackDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DecoderSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DisplayModeSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PlaybackTranscodeDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleDialog
import com.hritwik.avoid.presentation.ui.state.TrackChangeEvent
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.constants.PreferenceConstants.SKIP_PROMPT_FLOATING_DURATION_MS
import com.hritwik.avoid.utils.extensions.getSubtitleUrl
import com.hritwik.avoid.utils.extensions.showToast
import com.hritwik.avoid.utils.extensions.toSubtitleFileExtension
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import androidx.media3.common.MediaItem as ExoMediaItem

@OptIn(UnstableApi::class)
@Composable
fun ExoPlayerView(
    mediaItem: MediaItem,
    decoderMode: DecoderMode,
    displayMode: DisplayMode,
    userId: String,
    accessToken: String,
    serverUrl: String,
    autoSkipSegments: Boolean,
    gesturesEnabled: Boolean,
    onBackClick: () -> Unit,
    viewModel: VideoPlaybackViewModel,
    userDataViewModel: UserDataViewModel
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val playerState by viewModel.state.collectAsStateWithLifecycle()
    val currentMediaItem = playerState.mediaItem ?: mediaItem
    val latestMediaItem by rememberUpdatedState(currentMediaItem)
    val hasNextEpisode by rememberUpdatedState(playerState.hasNextEpisode)
    val playbackOffsetMs by rememberUpdatedState(playerState.playbackOffsetMs)
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioFocusRequest = rememberAudioFocusRequest(audioManager)
    val touchSlop = LocalViewConfiguration.current.touchSlop
    var showControls by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(true) }
    var resumeOnStart by remember { mutableStateOf(false) }
    var playbackDuration by remember { mutableLongStateOf(1L) }
    val overrideDurationSeconds = playerState.totalDurationSeconds?.takeIf { it > 0 }
    val effectiveDuration by rememberUpdatedState(overrideDurationSeconds ?: playbackDuration)
    var playbackProgress by remember { mutableLongStateOf(0L) }
    var volume by remember { mutableLongStateOf(playerState.volume) }
    var speed by remember { mutableFloatStateOf(playerState.playbackSpeed) }
    var isBuffering by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var exoPlayerRef by remember { mutableStateOf<ExoPlayer?>(null) }
    var playerReleased by remember { mutableStateOf(false) }
    val window = activity?.window
    var brightness by remember {
        mutableFloatStateOf(window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f)
    }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    val audioStreams = playerState.availableAudioStreams
    val subtitleStreams = playerState.availableSubtitleStreams
    val currentAudioTrack = playerState.audioStreamIndex
    val currentSubtitleTrack = playerState.subtitleStreamIndex
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showDisplayDialog by remember { mutableStateOf(false) }
    var showTranscodeDialog by remember { mutableStateOf(false) }
    var resumeAfterTrackChange by remember(currentMediaItem.id) { mutableStateOf(false) }
    var pendingTrackChangeEvent by remember { mutableStateOf<TrackChangeEvent?>(null) }
    var restorePositionMs by remember(currentMediaItem.id) {
        mutableLongStateOf(playerState.startPositionMs)
    }

    var skipLabel by remember { mutableStateOf("") }
    var currentSkipSegmentId by remember { mutableStateOf<String?>(null) }
    var skipSegmentEndMs by remember { mutableLongStateOf(0L) }
    var showOverlaySkipButton by remember { mutableStateOf(false) }
    var showFloatingSkipButton by remember { mutableStateOf(false) }
    var skipPromptDeadlineMs by remember { mutableLongStateOf(0L) }

    DisposableEffect(audioManager) {
        audioManager.requestAudioFocus(audioFocusRequest)
        onDispose { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
    }

    LaunchedEffect(playerState.totalDurationSeconds) {
        val overrideSeconds = playerState.totalDurationSeconds
        if (overrideSeconds != null && overrideSeconds > 0) {
            playbackDuration = overrideSeconds
        }
    }

    LaunchedEffect(audioStreams.size) {
        if (audioStreams.isEmpty()) {
            context.showToast("No audio tracks available")
        }
    }

    LaunchedEffect(subtitleStreams.size) {
        if (subtitleStreams.isEmpty()) {
            context.showToast("No subtitles available")
        }
    }

    val isPlayingState by rememberUpdatedState(isPlaying)

    LaunchedEffect(Unit) {
        viewModel.trackChangeEvents.collect { event ->
            resumeAfterTrackChange = isPlayingState
            pendingTrackChangeEvent = event
        }
    }

    LaunchedEffect(playerState.activeSegment, autoSkipSegments) {
        val segment = playerState.activeSegment
        if (segment != null && (
                    segment.type.equals("Intro", true) ||
                    segment.type.equals("Outro", true) ||
                    segment.type.equals("Credits", true)
                )
        ) {
            skipLabel = when {
                segment.type.equals("Intro", true) -> "Skip Intro"
                segment.type.equals("Outro", true) -> "Skip Outro"
                else -> "Skip Credits"
            }
            if (autoSkipSegments) {
                viewModel.skipSegment()
                currentSkipSegmentId = null
                skipSegmentEndMs = 0L
                skipPromptDeadlineMs = 0L
                showOverlaySkipButton = false
                showFloatingSkipButton = false
            } else {
                val startMs = segment.startPositionTicks / 10_000
                val endMs = segment.endPositionTicks / 10_000
                val currentPositionMs = playbackProgress * 1000
                val baseDeadline = startMs + SKIP_PROMPT_FLOATING_DURATION_MS
                val dynamicDeadline = currentPositionMs + SKIP_PROMPT_FLOATING_DURATION_MS
                val targetDeadline = max(baseDeadline, dynamicDeadline)
                currentSkipSegmentId = segment.id
                skipSegmentEndMs = endMs
                skipPromptDeadlineMs = if (endMs > 0L) {
                    min(targetDeadline, endMs)
                } else {
                    targetDeadline
                }
                showFloatingSkipButton = true
                showOverlaySkipButton = false
                showControls = false
            }
        } else {
            skipLabel = ""
            currentSkipSegmentId = null
            skipSegmentEndMs = 0L
            skipPromptDeadlineMs = 0L
            showOverlaySkipButton = false
            showFloatingSkipButton = false
        }
    }
    LaunchedEffect(
        playbackProgress,
        currentSkipSegmentId,
        skipSegmentEndMs,
        skipPromptDeadlineMs,
        playerState.activeSegment
    ) {
        val segmentId = currentSkipSegmentId ?: return@LaunchedEffect
        val currentMs = playbackProgress * 1000
        val promptDeadline = skipPromptDeadlineMs
        if (promptDeadline > 0 && currentMs >= promptDeadline) {
            skipPromptDeadlineMs = 0L
            showFloatingSkipButton = false
            showOverlaySkipButton = true
        }

        val endMs = skipSegmentEndMs
        val activeId = playerState.activeSegment?.id
        if ((endMs > 0 && currentMs >= endMs) || activeId != segmentId) {
            currentSkipSegmentId = null
            skipSegmentEndMs = 0L
            skipPromptDeadlineMs = 0L
            showOverlaySkipButton = false
            showFloatingSkipButton = false
        }
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(1000)
            gestureFeedback = null
        }
    }

    LaunchedEffect(playbackProgress) {
        restorePositionMs = playbackProgress * 1000
    }


    val windowInsetsController = remember(activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    LaunchedEffect(isPlaying, exoPlayerRef) {
        if (isPlaying) {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                exoPlayerRef?.pause()
                isPlaying = false
            }
        } else {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose { windowInsetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    LaunchedEffect(showControls, isSeeking) {
        if (showControls) {
            if (isSeeking) return@LaunchedEffect
            delay(5000L)
            if (!isSeeking) {
                showControls = false
            }
        }
    }

    LaunchedEffect(isPlaying, userId, accessToken, currentMediaItem.id) {
        if (!isPlaying) return@LaunchedEffect
        while (isPlaying) {
            if (!isSeeking) {
                viewModel.savePlaybackPosition(
                    mediaId = currentMediaItem.id,
                    userId = userId,
                    accessToken = accessToken,
                    positionSeconds = playbackProgress
                )
            }
            delay(1000L)
        }
    }

    BackHandler {
        viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
        viewModel.reportPlaybackStop(
            mediaId = currentMediaItem.id,
            userId = userId,
            accessToken = accessToken,
            positionSeconds = playbackProgress,
            isCompleted = playbackProgress >= effectiveDuration - 5
        )
        onBackClick()
    }

    DisposableEffect(Unit) {
        viewModel.reportPlaybackStart(currentMediaItem.id, userId, accessToken)
        onDispose {
            viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
            viewModel.reportPlaybackStop(
                mediaId = currentMediaItem.id,
                userId = userId,
                accessToken = accessToken,
                positionSeconds = playbackProgress,
                isCompleted = playbackProgress >= effectiveDuration - 5
            )
        }
    }

    DisposableEffect(lifecycleOwner, activity, exoPlayerRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isInPictureInPictureMode == true) {
                showControls = false
            }
            if (event == Lifecycle.Event.ON_RESUME && activity?.isInPictureInPictureMode == false) {
                showControls = true
            }
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && activity?.isInPictureInPictureMode == false) {
                resumeOnStart = isPlaying
                if (resumeOnStart) {
                    exoPlayerRef?.pause()
                    isPlaying = false
                }
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
            }
            if ((event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) && activity?.isInPictureInPictureMode == false) {
                if (resumeOnStart) {
                    val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
                    if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        exoPlayerRef?.play()
                        isPlaying = true
                    }
                    resumeOnStart = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val isLocalFile = remember(playerState.videoUrl) {
            playerState.videoUrl?.let {
                val uri = it.toUri()
                uri.scheme == "file" || File(uri.path ?: "").exists()
            } ?: false
        }
        val canTranscode = !isLocalFile && playerState.playbackTranscodeOptions.size > 1
        val baseItem = remember(playerState.videoUrl, playerState.exoMediaItem) {
            playerState.exoMediaItem ?: playerState.videoUrl?.let { ExoMediaItem.fromUri(it) }
        }

        val selectedSubtitleStream = remember(playerState.subtitleStreamIndex, subtitleStreams) {
            subtitleStreams.firstOrNull { it.index == playerState.subtitleStreamIndex }
        }

        val mediaItemWithSubtitles = remember(baseItem, selectedSubtitleStream, isLocalFile) {
            baseItem?.let { mediaItemBase ->
                when {
                    selectedSubtitleStream == null && !isLocalFile -> {
                        mediaItemBase.buildUpon()
                            .setSubtitleConfigurations(emptyList())
                            .build()
                    }

                    selectedSubtitleStream?.isExternal == true -> {
                        val config = buildSubtitleConfiguration(
                            context = context,
                            mediaItem = currentMediaItem,
                            stream = selectedSubtitleStream,
                            isLocalFile = isLocalFile,
                            serverUrl = serverUrl,
                            accessToken = accessToken
                        )
                        mediaItemBase.buildUpon()
                            .setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                            .build()
                    }

                    else -> mediaItemBase
                }
            }
        }

        val exoPlayer = remember(decoderMode, playerState.cacheDataSourceFactory) {
            playerReleased = false
            val renderersFactory = createRenderersFactory(context, decoderMode)
            val builder = ExoPlayer.Builder(context, renderersFactory)
            playerState.cacheDataSourceFactory?.let { factory ->
                builder.setMediaSourceFactory(DefaultMediaSourceFactory(factory))
            }
            builder.build().apply {
                playWhenReady = true
                volume = (playerState.volume.toFloat() / 100f).coerceIn(0f, 1f).toLong()
                setPlaybackSpeed(playerState.playbackSpeed)
            }
        }

        mediaItemWithSubtitles?.let { resolvedMediaItem ->
            LaunchedEffect(resolvedMediaItem, exoPlayer, playerReleased) {
                if (playerReleased) return@LaunchedEffect
                val shouldPlay = isPlaying
                exoPlayer.setMediaItem(resolvedMediaItem)
                exoPlayer.prepare()
                exoPlayer.seekTo(restorePositionMs)
                exoPlayer.playWhenReady = shouldPlay
            }

            LaunchedEffect(pendingTrackChangeEvent, isLocalFile, resolvedMediaItem, playerReleased) {
                val event = pendingTrackChangeEvent ?: return@LaunchedEffect
                if (playerReleased) return@LaunchedEffect
                val currentPositionMs = exoPlayer.currentPosition
                val shouldResumePlayback = when {
                    resumeAfterTrackChange -> true
                    else -> exoPlayer.playWhenReady || exoPlayer.isPlaying
                }
                restorePositionMs = currentPositionMs
                event.audioIndex?.let { index ->
                    val stream = audioStreams.firstOrNull { it.index == index }
                    exoPlayer.applyLocalAudioSelection(index, stream?.language)
                }
                if (isLocalFile) {
                    val updatedItem = exoPlayer.applyLocalSubtitleSelection(
                        targetIndex = event.subtitleIndex,
                        subtitleStreams = subtitleStreams,
                        mediaItem = currentMediaItem,
                        context = context,
                        serverUrl = serverUrl,
                        accessToken = accessToken,
                        isLocalFile = true
                    )
                    updatedItem?.let { item ->
                        exoPlayer.setMediaItem(item)
                        exoPlayer.prepare()
                    }
                } else {
                    val baseItem = playerState.videoUrl?.let { ExoMediaItem.fromUri(it) }
                    val updatedItem = when {
                        event.subtitleIndex == null -> {
                            exoPlayer.configureTextTrackPreferences(stream = null, enabled = false)
                            baseItem?.buildUpon()
                                ?.setSubtitleConfigurations(emptyList())
                                ?.build()
                        }

                        else -> {
                            val stream = subtitleStreams.firstOrNull { it.index == event.subtitleIndex }
                            when {
                                stream == null -> {
                                    exoPlayer.configureTextTrackPreferences(stream = null, enabled = false)
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(emptyList())
                                        ?.build()
                                }

                                stream.isExternal -> {
                                    exoPlayer.configureTextTrackPreferences(stream, enabled = true)
                                    val config = buildSubtitleConfiguration(
                                        context = context,
                                        mediaItem = currentMediaItem,
                                        stream = stream,
                                        isLocalFile = false,
                                        serverUrl = serverUrl,
                                        accessToken = accessToken
                                    )
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                        ?.build()
                                }
                                else -> {
                                    exoPlayer.configureTextTrackPreferences(stream, enabled = true)
                                    baseItem
                                }
                            }
                        }
                    }
                    updatedItem?.let { item ->
                        exoPlayer.setMediaItem(item)
                        exoPlayer.prepare()
                    }
                }

                exoPlayer.seekTo(currentPositionMs)
                exoPlayer.playWhenReady = shouldResumePlayback
                pendingTrackChangeEvent = null
                resumeAfterTrackChange = false
            }

            val pipParams = remember(exoPlayer.videoFormat, playerReleased) {
                val format = if (!playerReleased) exoPlayer.videoFormat else null
                val aspectRatio =
                    if (format != null && format.width > 0 && format.height > 0) {
                        Rational(format.width, format.height)
                    } else {
                        Rational(16, 9)
                    }
                PictureInPictureParams.Builder()
                    .setAspectRatio(aspectRatio)
                    .build()
            }

            val playbackListener = remember(exoPlayer) {
                object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            viewModel.reportPlaybackStop(
                                mediaId = latestMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = effectiveDuration,
                                isCompleted = true
                            )
                            viewModel.markAsWatched(latestMediaItem.id, userId)
                            if (hasNextEpisode) {
                                viewModel.playNextEpisode()
                            } else {
                                onBackClick()
                                audioManager.abandonAudioFocusRequest(audioFocusRequest)
                            }
                            isPlaying = false
                            viewModel.updatePausedState(true)
                            viewModel.updateBufferingState(false)
                        }
                        if (state == Player.STATE_BUFFERING) {
                            isBuffering = true
                            viewModel.updateBufferingState(true)
                        } else if (state == Player.STATE_READY || state == Player.STATE_ENDED || state == Player.STATE_IDLE) {
                            isBuffering = false
                            viewModel.updateBufferingState(false)
                        }
                    }

                    override fun onIsPlayingChanged(isPlayingState: Boolean) {
                        isPlaying = isPlayingState
                        viewModel.updatePausedState(!isPlayingState)
                    }
                }
            }

            var playerView by remember { mutableStateOf<PlayerView?>(null) }

            DisposableEffect(playerView) {
                playerView?.keepScreenOn = true
                onDispose { playerView?.keepScreenOn = false }
            }

            DisposableEffect(exoPlayer) {
                exoPlayerRef = exoPlayer
                exoPlayer.addListener(playbackListener)
                onDispose {
                    playerReleased = true
                    restorePositionMs = exoPlayer.currentPosition
                    exoPlayer.removeListener(playbackListener)
                    if (exoPlayerRef === exoPlayer) {
                        exoPlayerRef = null
                    }
                    audioManager.abandonAudioFocusRequest(audioFocusRequest)
                    exoPlayer.release()
                }
            }

            LaunchedEffect(playerState.startPositionMs, playerState.startPositionUpdateCount, playerReleased) {
                if (playerReleased) return@LaunchedEffect
                restorePositionMs = playerState.startPositionMs
                exoPlayer.seekTo(playerState.startPositionMs)
                playbackProgress = (playerState.startPositionMs + playbackOffsetMs) / 1000
            }

            LaunchedEffect(exoPlayer, playerReleased, playerState.totalDurationSeconds) {
                if (playerReleased) return@LaunchedEffect
                var lastPausedState: Boolean? = null
                var lastBufferingState: Boolean? = null
                while (isActive && !playerReleased) {
                    val overrideDuration = playerState.totalDurationSeconds
                    if (overrideDuration != null && overrideDuration > 0) {
                        playbackDuration = overrideDuration
                    } else {
                        val durationMs = exoPlayer.duration
                        if (durationMs > 0) {
                            playbackDuration = durationMs / 1000
                        }
                    }
                    if (!isSeeking) {
                        playbackProgress = (exoPlayer.currentPosition + playbackOffsetMs) / 1000
                    }
                    val currentlyPlaying = exoPlayer.isPlaying
                    if (isPlaying != currentlyPlaying) {
                        isPlaying = currentlyPlaying
                    }
                    val pausedState = !currentlyPlaying
                    if (lastPausedState != pausedState) {
                        viewModel.updatePausedState(pausedState)
                        lastPausedState = pausedState
                    }
                    val bufferingState = exoPlayer.playbackState == Player.STATE_BUFFERING
                    if (isBuffering != bufferingState) {
                        isBuffering = bufferingState
                    }
                    if (lastBufferingState != bufferingState) {
                        viewModel.updateBufferingState(bufferingState)
                        lastBufferingState = bufferingState
                    }
                    delay(500)
                }
            }

            LaunchedEffect(isPlaying, playerReleased) {
                if (!isPlaying || playerReleased) return@LaunchedEffect
                while (isActive && isPlaying && !playerReleased) {
                    viewModel.updatePlaybackPosition(exoPlayer.currentPosition + playbackOffsetMs)
                    delay(500L)
                }
            }

            LaunchedEffect(exoPlayerRef, selectedSubtitleStream, isLocalFile, playerReleased) {
                val player = exoPlayerRef ?: return@LaunchedEffect
                if (playerReleased) return@LaunchedEffect
                when {
                    selectedSubtitleStream == null -> {
                        player.configureTextTrackPreferences(stream = null, enabled = false)
                    }

                    selectedSubtitleStream.isExternal -> {
                        player.configureTextTrackPreferences(selectedSubtitleStream, enabled = true)
                    }

                    else -> {
                        player.configureTextTrackPreferences(selectedSubtitleStream, enabled = true)
                    }
                }
            }

            fun updateExoDisplayMode(playerView: PlayerView, mode: DisplayMode) {
                if (playerReleased) return
                when (mode) {
                    DisplayMode.FIT_SCREEN -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }

                    DisplayMode.CROP -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }

                    DisplayMode.STRETCH -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }

                    DisplayMode.ORIGINAL -> {
                        playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .let {
                        if (gesturesEnabled) it.pointerInput(playerReleased) {
                            if (playerReleased) return@pointerInput
                            val screenWidth = size.width.toFloat()
                            val screenHeight = size.height.toFloat()
                            val maxVolume =
                                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                        var startOffset = Offset.Zero
                        var startProgress = 0f
                        var startVolume = 0f
                        var startBrightness = 0f
                        var isHorizontal: Boolean? = null
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (playerReleased) return@detectDragGestures
                                startOffset = offset
                                startProgress = (exoPlayer.currentPosition + playbackOffsetMs) / 1000f
                                startVolume =
                                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                        .toFloat()
                                startBrightness = brightness
                                isHorizontal = null
                            },
                            onDrag = { change, _ ->
                                if (playerReleased) return@detectDragGestures
                                val dragDelta = change.position - startOffset
                                if (isHorizontal == null) {
                                    val absX = abs(dragDelta.x)
                                    val absY = abs(dragDelta.y)
                                    isHorizontal = if (absX > absY && absX > touchSlop) {
                                        true
                                    } else if (absY > absX && absY > touchSlop) {
                                        false
                                    } else {
                                        change.consume()
                                        return@detectDragGestures
                                    }
                                }
                                if (isHorizontal == true) {
                                    val delta = dragDelta.x / screenWidth * effectiveDuration
                                    val newPos = (startProgress + delta).coerceIn(
                                        0f,
                                        effectiveDuration.toFloat()
                                    )
                                    playbackProgress = newPos.roundToLong()
                                    val targetMs = (newPos * 1000).roundToLong()
                                    val durationMs = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                    val relativeMs = (targetMs - playbackOffsetMs).coerceIn(0L, durationMs)
                                    exoPlayer.seekTo(relativeMs)
                                    gestureFeedback = GestureFeedback.Seek(playbackProgress)
                                    isSeeking = true
                                } else {
                                    val factor = -dragDelta.y / screenHeight
                                    if (startOffset.x < screenWidth / 2f) {
                                        val newB = (startBrightness + factor).coerceIn(0f, 1f)
                                        brightness = newB
                                        window?.let { w ->
                                            val lp = w.attributes
                                            lp.screenBrightness = newB
                                            w.attributes = lp
                                        }
                                        gestureFeedback =
                                            GestureFeedback.Brightness((brightness * 100f).roundToInt())
                                    } else {
                                        val newV = (startVolume + factor * maxVolume).coerceIn(
                                            0f,
                                            maxVolume
                                        )
                                        audioManager.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            newV.roundToInt(),
                                            0
                                        )
                                        val percent = newV / maxVolume * 100f
                                        volume = percent.roundToLong()
                                        exoPlayer.volume = (newV / maxVolume).coerceIn(0f, 1f)
                                        viewModel.updateVolume(volume)
                                        gestureFeedback = GestureFeedback.Volume(volume.toInt())
                                    }
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                if (playerReleased) return@detectDragGestures
                                if (isHorizontal == true) {
                                    val targetMs = playbackProgress * 1000
                                    val requiresReload = viewModel.handleSeekRequest(targetMs)
                                    if (!requiresReload) {
                                        val durationMs = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                                        val relativeMs = (targetMs - playbackOffsetMs).coerceIn(0L, durationMs)
                                        exoPlayer.seekTo(relativeMs)
                                        restorePositionMs = relativeMs
                                    } else {
                                        restorePositionMs = 0
                                    }
                                    viewModel.savePlaybackPosition(
                                        currentMediaItem.id,
                                        userId,
                                        accessToken,
                                        playbackProgress
                                    )
                                    isSeeking = false
                                }
                                gestureFeedback = null
                            },
                            onDragCancel = {
                                if (playerReleased) return@detectDragGestures
                                isSeeking = false
                                gestureFeedback = null
                            }
                        )
                    } else it
                    }
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showControls = !showControls
                    }
                    .let {
                        if (gesturesEnabled) it.pointerInput(playerReleased) {
                            if (playerReleased) return@pointerInput
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (playerReleased) return@detectTapGestures
                                    val half = size.width / 2
                                    if (offset.x < half) {
                                        exoPlayer.seekTo(
                                            (exoPlayer.currentPosition - 10_000).coerceAtLeast(
                                                0
                                            )
                                        )
                                        gestureFeedback = GestureFeedback.Text("-10s")
                                    } else {
                                        exoPlayer.seekTo(exoPlayer.currentPosition + 10_000)
                                        gestureFeedback = GestureFeedback.Text("+10s")
                                    }
                                },
                                onTap = {
                                    if (playerReleased) return@detectTapGestures
                                    showControls = !showControls
                                }
                            )
                        } else it
                    },
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        subtitleView?.apply {
                            setApplyEmbeddedStyles(false)
                            setApplyEmbeddedFontSizes(false)
                            val captionStyle = CaptionStyleCompat(
                                Color.White.toArgb(),
                                Color.Transparent.toArgb(),
                                Color.Transparent.toArgb(),
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                Color.Black.toArgb(),
                                Typeface.SANS_SERIF
                            )
                            setStyle(captionStyle)
                            setFractionalTextSize(0.04f)
                            visibility = View.VISIBLE
                            z = Float.MAX_VALUE
                        }

                        updateExoDisplayMode(this, displayMode)
                    }.also { playerView = it }
                },
                update = { view ->
                    if (!playerReleased) {
                        updateExoDisplayMode(view, displayMode)
                    }
                    view.subtitleView?.apply {
                        visibility = if (selectedSubtitleStream != null) View.VISIBLE else View.GONE
                        z = Float.MAX_VALUE
                    }
                }
            )

            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                VideoControlsOverlay(
                    mediaTitle = currentMediaItem.name,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    duration = effectiveDuration,
                    currentPosition = playbackProgress,
                    onPlayPauseClick = {
                        if (playerReleased) return@VideoControlsOverlay
                        if (exoPlayer.isPlaying) {
                            viewModel.savePlaybackPosition(
                                currentMediaItem.id,
                                userId,
                                accessToken,
                                playbackProgress
                            )
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    onSeek = { position ->
                        isSeeking = true
                        playbackProgress = position
                    },
                    onSeekComplete = { position ->
                        if (playerReleased) return@VideoControlsOverlay
                        val targetMs = position * 1000
                        val requiresReload = viewModel.handleSeekRequest(targetMs)
                        if (!requiresReload) {
                            val durationMs = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            val relativeMs = (targetMs - playbackOffsetMs).coerceIn(0L, durationMs)
                            exoPlayer.seekTo(relativeMs)
                            restorePositionMs = relativeMs
                        } else {
                            restorePositionMs = 0
                        }
                        playbackProgress = position
                        isSeeking = false
                        viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, position)
                    },
                    onBackClick = {
                        viewModel.savePlaybackPosition(
                            currentMediaItem.id,
                            userId,
                            accessToken,
                            playbackProgress
                        )
                        viewModel.reportPlaybackStop(
                            mediaId = currentMediaItem.id,
                            userId = userId,
                            accessToken = accessToken,
                            positionSeconds = playbackProgress,
                            isCompleted = playbackProgress >= effectiveDuration - 5
                        )
                        onBackClick()
                    },
                    onSkipBackward = {
                        if (playerReleased) return@VideoControlsOverlay
                        val currentAbsoluteMs = exoPlayer.currentPosition + playbackOffsetMs
                        val targetMs = (currentAbsoluteMs - 10_000).coerceAtLeast(0)
                        val requiresReload = viewModel.handleSeekRequest(targetMs)
                        if (!requiresReload) {
                            val durationMs = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            val relativeMs = (targetMs - playbackOffsetMs).coerceIn(0L, durationMs)
                            exoPlayer.seekTo(relativeMs)
                            restorePositionMs = relativeMs
                        } else {
                            restorePositionMs = 0
                        }
                        playbackProgress = targetMs / 1000
                    },
                    onSkipForward = {
                        if (playerReleased) return@VideoControlsOverlay
                        val currentAbsoluteMs = exoPlayer.currentPosition + playbackOffsetMs
                        val maxDurationMs = effectiveDuration * 1000
                        val targetMs = min(currentAbsoluteMs + 10_000, maxDurationMs)
                        val requiresReload = viewModel.handleSeekRequest(targetMs)
                        if (!requiresReload) {
                            val durationMs = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
                            val relativeMs = (targetMs - playbackOffsetMs).coerceIn(0L, durationMs)
                            exoPlayer.seekTo(relativeMs)
                            restorePositionMs = relativeMs
                        } else {
                            restorePositionMs = 0
                        }
                        playbackProgress = targetMs / 1000
                    },
                    skipButtonVisible = showOverlaySkipButton,
                    skipButtonLabel = skipLabel,
                    onSkipButtonClick = {
                        viewModel.skipSegment()
                        currentSkipSegmentId = null
                        skipSegmentEndMs = 0L
                        skipPromptDeadlineMs = 0L
                        showOverlaySkipButton = false
                        showFloatingSkipButton = false
                        showControls = false
                    },
                    onPlayPrevious = if (playerState.hasPreviousEpisode) {
                        {
                            viewModel.savePlaybackPosition(
                                currentMediaItem.id,
                                userId,
                                accessToken,
                                playbackProgress
                            )
                            viewModel.reportPlaybackStop(
                                mediaId = currentMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = playbackProgress,
                                isCompleted = playbackProgress >= effectiveDuration - 5
                            )
                            viewModel.playPreviousEpisode()
                        }
                    } else {
                        null
                    },
                    onPlayNext = if (playerState.hasNextEpisode) {
                        {
                            viewModel.savePlaybackPosition(
                                currentMediaItem.id,
                                userId,
                                accessToken,
                                playbackProgress
                            )
                            viewModel.reportPlaybackStop(
                                mediaId = currentMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = playbackProgress,
                                isCompleted = playbackProgress >= effectiveDuration - 5
                            )
                            viewModel.playNextEpisode()
                        }
                    } else {
                        null
                    },
                    speed = speed,
                    onSpeedChange = { newSpeed ->
                        if (playerReleased) return@VideoControlsOverlay
                        speed = newSpeed
                        exoPlayer.setPlaybackSpeed(newSpeed)
                        viewModel.updatePlaybackSpeed(newSpeed)
                    },
                    playbackQualityLabel = playerState.currentTranscodeQualityText,
                    onPlaybackQualityClick = if (canTranscode) {
                        { showTranscodeDialog = true }
                    } else {
                        null
                    },
                    onAudioClick = if (audioStreams.isNotEmpty()) {
                        { showAudioDialog = true }
                    } else {
                        null
                    },
                    onSubtitleClick = if (subtitleStreams.isNotEmpty()) {
                        { showSubtitleDialog = true }
                    } else {
                        null
                    },
                    onDecoderClick = { showDecoderDialog = true },
                    onDisplayClick = { showDisplayDialog = true },
                    onPipClick = {
                        activity?.enterPictureInPictureMode(pipParams)
                        showControls = false
                    }
                )
            }

            AnimatedVisibility(
                visible = showFloatingSkipButton && skipLabel.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = calculateRoundedValue(16).sdp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    TextButton(
                        onClick = {
                            viewModel.skipSegment()
                            currentSkipSegmentId = null
                            skipSegmentEndMs = 0L
                            skipPromptDeadlineMs = 0L
                            showOverlaySkipButton = false
                            showFloatingSkipButton = false
                            showControls = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = skipLabel)
                    }
                    Spacer(modifier = Modifier.height(calculateRoundedValue(56).sdp))
                }
            }

            if (showTranscodeDialog) {
                PlaybackTranscodeDialog(
                    options = playerState.playbackTranscodeOptions,
                    selectedOption = playerState.selectedPlaybackTranscodeOption,
                    onOptionSelected = { option ->
                        viewModel.selectPlaybackTranscodeOption(option)
                        showTranscodeDialog = false
                    },
                    onDismiss = { showTranscodeDialog = false }
                )
            }

            if (showAudioDialog) {
                AudioTrackDialog(
                    audioStreams = audioStreams,
                    selectedAudioStream = audioStreams.firstOrNull { it.index == currentAudioTrack },
                    onAudioSelected = { stream ->
                        if (playerReleased) return@AudioTrackDialog
                        val currentPosition = exoPlayer.currentPosition
                        viewModel.updateAudioStream(
                            stream.index,
                            currentMediaItem,
                            serverUrl,
                            shouldRebuildUrl = !isLocalFile,
                            accessToken = accessToken
                        )
                        restorePositionMs = currentPosition
                        exoPlayer.applyLocalAudioSelection(stream.index, stream.language)
                        if (isLocalFile) {
                            exoPlayer.seekTo(currentPosition)
                            exoPlayer.playWhenReady = true
                        } else {
                            val baseItem = viewModel.state.value.videoUrl?.let { url ->
                                ExoMediaItem.fromUri(url)
                            }
                            val subtitleStream = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack }
                            val mediaItem = if (subtitleStream?.isExternal == true) {
                                val config = buildSubtitleConfiguration(
                                    context = context,
                                    mediaItem = currentMediaItem,
                                    stream = subtitleStream,
                                    isLocalFile = false,
                                    serverUrl = serverUrl,
                                    accessToken = accessToken
                                )
                                baseItem?.buildUpon()
                                    ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                    ?.build()
                            } else {
                                baseItem
                            }
                            mediaItem?.let { item ->
                                exoPlayer.setMediaItem(item)
                                exoPlayer.prepare()
                                exoPlayer.seekTo(currentPosition)
                                exoPlayer.playWhenReady = true
                            }
                        }
                        showAudioDialog = false
                    },
                    onDismiss = { showAudioDialog = false }
                )
            }

            if (showSubtitleDialog) {
                SubtitleDialog(
                    subtitleStreams = subtitleStreams,
                    selectedSubtitleStream = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack },
                    onSubtitleSelected = { stream ->
                        if (playerReleased) return@SubtitleDialog
                        val currentPosition = exoPlayer.currentPosition
                        viewModel.updateSubtitleStream(
                            stream?.index,
                            currentMediaItem,
                            serverUrl,
                            shouldRebuildUrl = !isLocalFile,
                            accessToken = accessToken
                        )
                        restorePositionMs = currentPosition
                        if (isLocalFile) {
                            val updatedItem = exoPlayer.applyLocalSubtitleSelection(
                                targetIndex = stream?.index,
                                subtitleStreams = subtitleStreams,
                                mediaItem = currentMediaItem,
                                context = context,
                                serverUrl = serverUrl,
                                accessToken = accessToken,
                                isLocalFile = true
                            )
                            updatedItem?.let { item ->
                                exoPlayer.setMediaItem(item)
                                exoPlayer.prepare()
                            }
                            exoPlayer.seekTo(currentPosition)
                            exoPlayer.playWhenReady = true
                        } else {
                            val baseItem = viewModel.state.value.videoUrl?.let { url ->
                                ExoMediaItem.fromUri(url)
                            }
                            val finalItem = when {
                                stream == null -> {
                                    exoPlayer.configureTextTrackPreferences(stream = null, enabled = false)
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(emptyList())
                                        ?.build()
                                }

                                stream.isExternal -> {
                                    exoPlayer.configureTextTrackPreferences(stream, enabled = true)
                                    val config = buildSubtitleConfiguration(
                                        context = context,
                                        mediaItem = currentMediaItem,
                                        stream = stream,
                                        isLocalFile = false,
                                        serverUrl = serverUrl,
                                        accessToken = accessToken
                                    )
                                    baseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                        ?.build()
                                }

                                else -> baseItem
                            }
                            finalItem?.let { item ->
                                exoPlayer.setMediaItem(item)
                                exoPlayer.prepare()
                            }
                            exoPlayer.seekTo(currentPosition)
                            exoPlayer.playWhenReady = true
                        }
                        showSubtitleDialog = false
                    },
                    onDismiss = { showSubtitleDialog = false }
                )
            }

            if (showDisplayDialog) {
                DisplayModeSelectionDialog(
                    currentMode = displayMode,
                    onModeSelected = { mode ->
                        userDataViewModel.setDisplayMode(mode)
                        showDisplayDialog = false
                    },
                    onDismiss = { showDisplayDialog = false }
                )
            }

            if (showDecoderDialog) {
                DecoderSelectionDialog(
                    currentMode = decoderMode,
                    onModeSelected = { mode ->
                        userDataViewModel.setDecoderMode(mode)
                        context.showToast("Decoder mode set to ${mode.name}")
                        showDecoderDialog = false
                    },
                    onDismiss = { showDecoderDialog = false }
                )
            }

            if (isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Minsk
                )
            }
        }

        GestureFeedbackOverlay(
            feedback = gestureFeedback,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@OptIn(UnstableApi::class)
private fun createRenderersFactory(
    context: Context,
    decoderMode: DecoderMode
): DefaultRenderersFactory {
    return DefaultRenderersFactory(context).apply {
        when (decoderMode) {
            DecoderMode.HARDWARE_ONLY -> {
                setEnableDecoderFallback(false)
            }

            DecoderMode.SOFTWARE_ONLY -> {
                setEnableDecoderFallback(true)
            }

            DecoderMode.AUTO -> {
                setEnableDecoderFallback(true)
            }
        }
    }
}

private fun ExoPlayer.findTrackOverride(
    trackType: Int,
    targetIndex: Int,
    targetLanguage: String? = null
): TrackSelectionOverride? {
    return currentTracks.groups.firstNotNullOfOrNull { group ->
        if (group.type == trackType) {
            val trackGroup = group.mediaTrackGroup
            (0 until group.length).firstNotNullOfOrNull { trackIndex ->
                val format = group.getTrackFormat(trackIndex)
                val formatId = format.id
                when {
                    formatId?.toIntOrNull() == targetIndex ->
                        TrackSelectionOverride(trackGroup, trackIndex)

                    formatId == targetIndex.toString() ->
                        TrackSelectionOverride(trackGroup, trackIndex)

                    !targetLanguage.isNullOrBlank() &&
                        !format.language.isNullOrBlank() &&
                        format.language.equals(targetLanguage, ignoreCase = true) ->
                        TrackSelectionOverride(trackGroup, trackIndex)

                    trackIndex == targetIndex ->
                        TrackSelectionOverride(trackGroup, trackIndex)

                    else -> null
                }
            }
        } else {
            null
        }
    }
}

private fun ExoPlayer.applyLocalAudioSelection(
    targetIndex: Int,
    targetLanguage: String? = null
) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)

    val override = findTrackOverride(C.TRACK_TYPE_AUDIO, targetIndex, targetLanguage)
    if (override != null) {
        builder.addOverride(override)
    } else {
        targetLanguage?.takeUnless { it.isBlank() }?.let { language ->
            builder.setPreferredAudioLanguage(language)
        }
    }
    trackSelectionParameters = builder.build()
}

private fun buildSubtitleConfiguration(
    context: Context,
    mediaItem: MediaItem,
    stream: MediaStream,
    isLocalFile: Boolean,
    serverUrl: String,
    accessToken: String
): ExoMediaItem.SubtitleConfiguration? {
    if (!stream.isExternal) return null

    val extension = stream.codec.toSubtitleFileExtension()
    val localFile = getLocalSubtitleFile(context, mediaItem.id, stream.index, extension)
    val subtitleUri = when {
        isLocalFile -> localFile?.toUri()
        else -> localFile?.toUri()
            ?: mediaItem.getSubtitleUrl(serverUrl, accessToken, stream.index).toUri()
    } ?: return null

    val mimeType = when (extension.lowercase()) {
        "srt" -> MimeTypes.APPLICATION_SUBRIP
        "sup" -> MimeTypes.APPLICATION_PGS
        "sub" -> MimeTypes.APPLICATION_VOBSUB
        "vtt" -> MimeTypes.TEXT_VTT
        "dvb" -> MimeTypes.APPLICATION_DVBSUBS
        "cea608" -> MimeTypes.APPLICATION_CEA608
        "cea708" -> MimeTypes.APPLICATION_CEA708
        "mp4vtt", "tx3g" -> MimeTypes.APPLICATION_MP4VTT
        "ass", "ssa" -> MimeTypes.TEXT_SSA
        "ttml" -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }

    return ExoMediaItem.SubtitleConfiguration.Builder(subtitleUri)
        .setMimeType(mimeType)
        .setLanguage(stream.language)
        .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
        .setRoleFlags(C.ROLE_FLAG_SUBTITLE)
        .build()
}

private fun ExoPlayer.configureTextTrackPreferences(stream: MediaStream?, enabled: Boolean) {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    if (enabled && stream != null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

        if (stream.isExternal) {
            stream.language?.takeUnless { it.isBlank() }?.let { language ->
                builder.setPreferredTextLanguage(language)
            }
        } else {
            val language = stream.language?.takeUnless { it.isBlank() }
            val override = findTrackOverride(C.TRACK_TYPE_TEXT, stream.index, language)
            if (override != null) {
                builder.addOverride(override)
            } else {
                language?.let { builder.setPreferredTextLanguage(it) }
            }
        }
    } else {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
    }

    trackSelectionParameters = builder.build()
}

private fun ExoPlayer.applyLocalSubtitleSelection(
    targetIndex: Int?,
    subtitleStreams: List<MediaStream>,
    mediaItem: MediaItem,
    context: Context,
    serverUrl: String,
    accessToken: String,
    isLocalFile: Boolean
): ExoMediaItem? {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_TEXT)

    if (targetIndex == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        trackSelectionParameters = builder.build()
        val currentItem = currentMediaItem
        val hasConfigs = currentItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true
        return if (hasConfigs) {
            currentItem.buildUpon()
                .setSubtitleConfigurations(emptyList())
                .build()
        } else {
            null
        }
    }

    val stream = subtitleStreams.firstOrNull { it.index == targetIndex }
    if (stream == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        trackSelectionParameters = builder.build()
        return null
    }

    builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)

    return if (stream.isExternal) {
        stream.language?.takeUnless { it.isBlank() }?.let { language ->
            builder.setPreferredTextLanguage(language)
        }
        trackSelectionParameters = builder.build()

        val config = buildSubtitleConfiguration(
            context = context,
            mediaItem = mediaItem,
            stream = stream,
            isLocalFile = isLocalFile,
            serverUrl = serverUrl,
            accessToken = accessToken
        )
        currentMediaItem?.buildUpon()
            ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
            ?.build()
    } else {
        val language = stream.language?.takeUnless { it.isBlank() }
        val override = findTrackOverride(C.TRACK_TYPE_TEXT, targetIndex, language)
        if (override != null) {
            builder.addOverride(override)
        } else {
            language?.let { builder.setPreferredTextLanguage(it) }
        }
        trackSelectionParameters = builder.build()

        val currentItem = currentMediaItem
        val hasConfigs = currentItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true
        if (hasConfigs) {
            currentItem.buildUpon()
                .setSubtitleConfigurations(emptyList())
                .build()
        } else {
            null
        }
    }
}

private fun getLocalSubtitleFile(
    context: Context,
    mediaId: String,
    index: Int,
    extension: String
): File? {
    val fileName = "$mediaId-$index.$extension"
    val internal = File(context.filesDir, "downloads/subtitles/$fileName")
    if (internal.exists()) return internal
    val externalDir = context.getExternalFilesDir(null)
    val external = externalDir?.let { File(it, "downloads/subtitles/$fileName") }
    return external?.takeIf { it.exists() }
}
