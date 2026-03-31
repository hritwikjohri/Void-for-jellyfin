package com.hritwik.avoid.presentation.ui.screen.player

import android.app.PictureInPictureParams
import android.content.Context
import android.graphics.Typeface
import android.media.AudioManager
import android.util.Rational
import android.view.View
import android.view.WindowManager
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.media3.common.Format
import androidx.media3.common.Metadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.ffmpeg.FilteringExtractorsFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ffmpeg.FfmpegFilteringMode
import androidx.media3.extractor.metadata.id3.ChapterFrame
import androidx.media3.extractor.metadata.id3.TextInformationFrame
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.HdrFormatPreference
import com.hritwik.avoid.domain.model.playback.Segment
import com.hritwik.avoid.presentation.ui.components.common.rememberAudioFocusRequest
import com.hritwik.avoid.presentation.ui.components.dialogs.AudioTrackDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DecoderSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.DisplayModeSelectionDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.PlaybackTranscodeDialog
import com.hritwik.avoid.presentation.ui.components.dialogs.SubtitleDialog
import com.hritwik.avoid.presentation.ui.state.TrackChangeEvent
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.ui.theme.resolvePlayerProgressColor
import com.hritwik.avoid.presentation.ui.theme.resolvePlayerProgressColorOrNull
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
import java.util.Locale
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
    autoPlayNextEpisode: Boolean,
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
    val playbackSettings by userDataViewModel.playbackSettings.collectAsStateWithLifecycle()
    val hdrFormatPreference = playbackSettings.hdrFormatPreference
    val progressBarColorKey by userDataViewModel.playerProgressColor.collectAsStateWithLifecycle()
    val seekProgressColorKey by userDataViewModel.playerProgressSeekColor.collectAsStateWithLifecycle()
    val progressBarColor = resolvePlayerProgressColor(progressBarColorKey)
    val seekProgressColor = resolvePlayerProgressColorOrNull(seekProgressColorKey)
    val autoPlayNext by rememberUpdatedState(autoPlayNextEpisode)
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
    var isScreenLocked by remember { mutableStateOf(false) }
    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    var exoPlayerRef by remember { mutableStateOf<ExoPlayer?>(null) }
    var playerReleased by remember { mutableStateOf(false) }
    val window = activity?.window
    val initialWindowBrightness = remember(window) {
        window?.attributes?.screenBrightness
            ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
    }
    var brightness by remember {
        mutableFloatStateOf(initialWindowBrightness.takeIf { it >= 0 } ?: 0.5f)
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
    var pendingAudioSelectionIndex by remember(currentMediaItem.id) { mutableStateOf<Int?>(null) }
    // FIX: mutableIntStateOf for primitive Int state
    var pendingAudioSelectionRetryCount by remember(currentMediaItem.id) { mutableIntStateOf(0) }
    var pendingSubtitleSelectionStream by remember(currentMediaItem.id) { mutableStateOf<MediaStream?>(null) }
    var pendingSubtitleSelectionRetryCount by remember(currentMediaItem.id) { mutableIntStateOf(0) }
    var restorePositionMs by remember(currentMediaItem.id) {
        mutableLongStateOf(playerState.startPositionMs)
    }

    // Single gesture feedback effect — duplicate removed
    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(700)
            gestureFeedback = null
        }
    }

    var skipLabel by remember { mutableStateOf("") }
    var currentSkipSegmentId by remember { mutableStateOf<String?>(null) }
    var skipSegmentEndMs by remember { mutableLongStateOf(0L) }
    var showOverlaySkipButton by remember { mutableStateOf(false) }
    var showFloatingSkipButton by remember { mutableStateOf(false) }
    var skipPromptDeadlineMs by remember { mutableLongStateOf(0L) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }

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
        if (audioStreams.isEmpty()) context.showToast("No audio tracks available")
    }

    LaunchedEffect(subtitleStreams.size) {
        if (subtitleStreams.isEmpty()) context.showToast("No subtitles available")
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
                skipPromptDeadlineMs = if (endMs > 0L) min(targetDeadline, endMs) else targetDeadline
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
        if (promptDeadline in 1..currentMs) {
            skipPromptDeadlineMs = 0L
            showFloatingSkipButton = false
            showOverlaySkipButton = true
        }
        val endMs = skipSegmentEndMs
        val activeId = playerState.activeSegment?.id
        if ((endMs in 1..currentMs) || activeId != segmentId) {
            currentSkipSegmentId = null
            skipSegmentEndMs = 0L
            skipPromptDeadlineMs = 0L
            showOverlaySkipButton = false
            showFloatingSkipButton = false
        }
    }

    LaunchedEffect(playbackProgress) {
        restorePositionMs = playbackProgress * 1000
    }

    // FIX: renamed inner lambda param from `it` to `w` to avoid shadowing
    val windowInsetsController = remember(activity) {
        activity?.window?.let { w -> WindowCompat.getInsetsController(w, w.decorView) }
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
        if (showControls) windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        else windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
    }

    DisposableEffect(Unit) {
        onDispose { windowInsetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }

    LaunchedEffect(showControls, isSeeking) {
        if (showControls) {
            if (isSeeking) return@LaunchedEffect
            delay(5000L)
            if (!isSeeking) showControls = false
        }
    }

    LaunchedEffect(isPlaying, userId, accessToken, currentMediaItem.id) {
        if (!isPlaying) return@LaunchedEffect
        var lastReportedPosition = 0L
        while (isPlaying) {
            if (!isSeeking) {
                val currentPosition = playbackProgress
                val positionDiff = abs(currentPosition - lastReportedPosition)
                if (positionDiff >= 10) {
                    viewModel.savePlaybackPosition(
                        mediaId = currentMediaItem.id,
                        userId = userId,
                        accessToken = accessToken,
                        positionSeconds = currentPosition
                    )
                    lastReportedPosition = currentPosition
                }
            }
            delay(5000L)
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

    DisposableEffect(lifecycleOwner, activity, exoPlayerRef, playerView) {
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
                playerView?.onPause()
            }
            if ((event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) && activity?.isInPictureInPictureMode == false) {
                playerView?.onResume()
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

    DisposableEffect(window) {
        onDispose {
            window?.let { w ->
                val lp = w.attributes
                lp.screenBrightness = initialWindowBrightness
                w.attributes = lp
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // FIX: renamed inner `it` to `url` to avoid shadowing
        val isLocalFile = remember(playerState.videoUrl) {
            playerState.videoUrl?.let { url ->
                val uri = url.toUri()
                uri.scheme == "file" || File(uri.path ?: "").exists()
            } ?: false
        }
        val canTranscode = !isLocalFile && playerState.playbackTranscodeOptions.size > 1
        val baseItem = remember(playerState.videoUrl, playerState.exoMediaItem) {
            playerState.exoMediaItem ?: playerState.videoUrl?.let { url -> ExoMediaItem.fromUri(url) }
        }

        val selectedSubtitleStream = remember(playerState.subtitleStreamIndex, subtitleStreams) {
            subtitleStreams.firstOrNull { it.index == playerState.subtitleStreamIndex }
        }

        val mediaItemWithSubtitles = remember(baseItem, selectedSubtitleStream, isLocalFile) {
            baseItem?.let { mediaItemBase ->
                when {
                    selectedSubtitleStream == null && !isLocalFile -> {
                        mediaItemBase.buildUpon().setSubtitleConfigurations(emptyList()).build()
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

        val exoPlayer = remember(decoderMode, playerState.cacheDataSourceFactory, hdrFormatPreference) {
            playerReleased = false
            val filteringMode = when (hdrFormatPreference) {
                HdrFormatPreference.HDR10_PLUS -> FfmpegFilteringMode.KEEP_HDR10_BASE
                HdrFormatPreference.DOLBY_VISION -> FfmpegFilteringMode.KEEP_DOLBY_VISION
                HdrFormatPreference.DOLBY_VISION_MEL -> FfmpegFilteringMode.KEEP_DOLBY_VISION_MEL
                else -> FfmpegFilteringMode.AUTO
            }
            val extractorsFactory = FilteringExtractorsFactory(DefaultExtractorsFactory(), filteringMode)
            val mediaSourceFactory = playerState.cacheDataSourceFactory?.let { factory ->
                DefaultMediaSourceFactory(factory, extractorsFactory)
            } ?: DefaultMediaSourceFactory(context, extractorsFactory)
            val renderersFactory = createRenderersFactory(context, decoderMode, hdrFormatPreference)
            ExoPlayer.Builder(context, renderersFactory)
                .setMediaSourceFactory(mediaSourceFactory)
                .build().apply {
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
                currentAudioTrack?.let { selectedIndex ->
                    pendingAudioSelectionIndex = selectedIndex
                    pendingAudioSelectionRetryCount = 0
                }
            }

            LaunchedEffect(pendingTrackChangeEvent, isLocalFile, resolvedMediaItem, playerReleased) {
                val event = pendingTrackChangeEvent ?: return@LaunchedEffect
                if (playerReleased) return@LaunchedEffect
                if (!isLocalFile) {
                    val currentUrl = playerState.videoUrl ?: return@LaunchedEffect
                    val parsedUrl = runCatching { currentUrl.toUri() }.getOrNull() ?: return@LaunchedEffect
                    val targetAudio = event.audioIndex?.toString()
                    val targetSubtitle = event.subtitleIndex?.toString()
                    val urlAudio = parsedUrl.getQueryParameter("AudioStreamIndex")
                    val urlSubtitle = parsedUrl.getQueryParameter("SubtitleStreamIndex")
                    if (targetAudio != null && urlAudio != targetAudio) return@LaunchedEffect
                    if (targetAudio == null && urlAudio != null) return@LaunchedEffect
                    if (targetSubtitle != null && urlSubtitle != targetSubtitle) return@LaunchedEffect
                    if (event.subtitleIndex == null && urlSubtitle != null) return@LaunchedEffect
                }
                val currentPositionMs = exoPlayer.currentPosition
                val shouldResumePlayback = resumeAfterTrackChange || exoPlayer.playWhenReady || exoPlayer.isPlaying
                restorePositionMs = currentPositionMs
                if (isLocalFile) {
                    // FIX: renamed `index` to `audioIdx` to avoid shadowing
                    event.audioIndex?.let { audioIdx ->
                        val stream = audioStreams.firstOrNull { s -> s.index == audioIdx }
                        val applied = exoPlayer.applyLocalAudioSelection(audioIdx, stream, audioStreams)
                        pendingAudioSelectionIndex = if (applied) null else audioIdx
                        pendingAudioSelectionRetryCount = 0
                    }
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
                    event.audioIndex?.let { audioIndex ->
                        val audioStream = audioStreams.firstOrNull { s -> s.index == audioIndex }
                        val applied = exoPlayer.configureAudioTrackPreferences(audioStream, audioStreams)
                        pendingAudioSelectionIndex = if (applied) null else audioIndex
                        pendingAudioSelectionRetryCount = 0
                    }
                    // Use the proxied exoMediaItem (mTLS-aware) instead of raw videoUrl
                    val remoteBaseItem = playerState.exoMediaItem
                        ?: playerState.videoUrl?.let { url -> ExoMediaItem.fromUri(url) }
                    val updatedItem = when {
                        event.subtitleIndex == null -> {
                            pendingSubtitleSelectionStream = null
                            pendingSubtitleSelectionRetryCount = 0
                            exoPlayer.configureTextTrackPreferences(stream = null, enabled = false)
                            if (exoPlayer.currentMediaItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true) {
                                remoteBaseItem?.buildUpon()?.setSubtitleConfigurations(emptyList())?.build()
                            } else null
                        }
                        else -> {
                            val stream = subtitleStreams.firstOrNull { s -> s.index == event.subtitleIndex }
                            when {
                                stream == null -> {
                                    pendingSubtitleSelectionStream = null
                                    pendingSubtitleSelectionRetryCount = 0
                                    exoPlayer.configureTextTrackPreferences(stream = null, enabled = false)
                                    if (exoPlayer.currentMediaItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true) {
                                        remoteBaseItem?.buildUpon()?.setSubtitleConfigurations(emptyList())?.build()
                                    } else null
                                }
                                stream.isExternal -> {
                                    pendingSubtitleSelectionStream = null
                                    pendingSubtitleSelectionRetryCount = 0
                                    exoPlayer.configureTextTrackPreferences(stream, enabled = true, subtitleStreams = subtitleStreams)
                                    val config = buildSubtitleConfiguration(
                                        context = context,
                                        mediaItem = currentMediaItem,
                                        stream = stream,
                                        isLocalFile = false,
                                        serverUrl = serverUrl,
                                        accessToken = accessToken
                                    )
                                    remoteBaseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                        ?.build()
                                }
                                else -> {
                                    exoPlayer.configureTextTrackPreferences(stream, enabled = true, subtitleStreams = subtitleStreams)
                                    pendingSubtitleSelectionStream = stream
                                    pendingSubtitleSelectionRetryCount = 0
                                    null
                                }
                            }
                        }
                    }
                    if (updatedItem != null) {
                        exoPlayer.setMediaItem(updatedItem)
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
                val aspectRatio = if (format != null && format.width > 0 && format.height > 0) {
                    Rational(format.width, format.height)
                } else {
                    Rational(16, 9)
                }
                PictureInPictureParams.Builder().setAspectRatio(aspectRatio).build()
            }

            val introChapterRegex = remember { Regex("(?i)(Opening Credits|intro|opening|^OP$)") }
            val outroChapterRegex = remember { Regex("(?i)(outro|closing|ending|^ED$)") }
            val creditsChapterRegex = remember { Regex("(?i)(End Credits)") }

            fun chapterTitleToSegmentType(title: String?): String? {
                val normalized = title?.trim().orEmpty()
                if (normalized.isEmpty()) return null
                return when {
                    creditsChapterRegex.containsMatchIn(normalized) -> "Credits"
                    outroChapterRegex.containsMatchIn(normalized) -> "Outro"
                    introChapterRegex.containsMatchIn(normalized) -> "Intro"
                    else -> null
                }
            }

            fun extractChapterTitle(frame: ChapterFrame): String? {
                for (i in 0 until frame.subFrameCount) {
                    val subFrame = frame.getSubFrame(i)
                    // FIX: use values.firstOrNull() — .value is deprecated
                    if (subFrame is TextInformationFrame && subFrame.id.equals("TIT2", true)) {
                        return subFrame.values.firstOrNull()
                    }
                }
                return null
            }

            val playbackListener = remember(exoPlayer, isLocalFile, audioStreams) {
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
                            if (hasNextEpisode && autoPlayNext) {
                                viewModel.playNextEpisode()
                            } else if (!hasNextEpisode) {
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

                    override fun onMetadata(metadata: Metadata) {
                        if (playerState.segments.isNotEmpty()) return
                        val chapterSegments = mutableListOf<Segment>()
                        // FIX: removed unused chapterCount variable
                        for (i in 0 until metadata.length()) {
                            val entry = metadata[i]
                            if (entry is ChapterFrame) {
                                val title = extractChapterTitle(entry)
                                val type = chapterTitleToSegmentType(title) ?: continue
                                val startMs = entry.startTimeMs.toLong()
                                val endMs = entry.endTimeMs.toLong()
                                if (startMs !in 0..<endMs) continue
                                chapterSegments.add(
                                    Segment(
                                        id = "chapter-${entry.chapterId}",
                                        startPositionTicks = startMs * 10_000L,
                                        endPositionTicks = endMs * 10_000L,
                                        type = type
                                    )
                                )
                            }
                        }
                        if (chapterSegments.isNotEmpty()) {
                            viewModel.applyChapterSegments(chapterSegments)
                            viewModel.updatePlaybackPosition(exoPlayer.currentPosition)
                        }
                    }

                    override fun onTracksChanged(tracks: Tracks) {
                        // FIX: renamed `it` to `group` to avoid shadowing
                        val hasAudioGroups = tracks.groups.any { group ->
                            group.type == C.TRACK_TYPE_AUDIO && group.length > 0
                        }
                        if (!hasAudioGroups) return
                        val pendingIndex = pendingAudioSelectionIndex
                        if (pendingIndex != null) {
                            pendingAudioSelectionIndex = null
                            val pendingStream = audioStreams.firstOrNull { s -> s.index == pendingIndex }
                            val applied = if (isLocalFile) {
                                exoPlayer.applyLocalAudioSelection(
                                    targetIndex = pendingIndex,
                                    targetStream = pendingStream,
                                    audioStreams = audioStreams
                                )
                            } else {
                                exoPlayer.configureAudioTrackPreferences(pendingStream, audioStreams)
                            }
                            if (applied) {
                                pendingAudioSelectionRetryCount = 0
                                Log.i("TrackMap", "Deferred audio apply succeeded jellyfinIndex=$pendingIndex")
                            } else {
                                val nextRetry = pendingAudioSelectionRetryCount + 1
                                if (nextRetry <= 5) {
                                    pendingAudioSelectionRetryCount = nextRetry
                                    pendingAudioSelectionIndex = pendingIndex
                                    Log.w("TrackMap", "Deferred audio apply failed jellyfinIndex=$pendingIndex retry=$nextRetry")
                                } else {
                                    pendingAudioSelectionRetryCount = 0
                                    Log.e("TrackMap", "Deferred audio apply aborted jellyfinIndex=$pendingIndex retriesExceeded=true")
                                }
                            }
                        }
                        // Deferred subtitle track selection (mirrors audio retry logic)
                        val pendingSubStream = pendingSubtitleSelectionStream
                        if (pendingSubStream != null && !pendingSubStream.isExternal) {
                            val hasTextGroups = tracks.groups.any { group ->
                                group.type == C.TRACK_TYPE_TEXT && group.length > 0
                            }
                            if (hasTextGroups) {
                                pendingSubtitleSelectionStream = null
                                val override = exoPlayer.findTextTrackOverride(pendingSubStream, subtitleStreams)
                                if (override != null) {
                                    val builder = exoPlayer.trackSelectionParameters.buildUpon()
                                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                        .addOverride(override)
                                    pendingSubStream.language?.takeUnless { it.isBlank() }?.let { lang ->
                                        builder.setPreferredTextLanguage(lang)
                                    }
                                    exoPlayer.trackSelectionParameters = builder.build()
                                    pendingSubtitleSelectionRetryCount = 0
                                    Log.i("TrackMap", "Deferred subtitle apply succeeded jellyfinIndex=${pendingSubStream.index}")
                                } else {
                                    val nextRetry = pendingSubtitleSelectionRetryCount + 1
                                    if (nextRetry <= 5) {
                                        pendingSubtitleSelectionRetryCount = nextRetry
                                        pendingSubtitleSelectionStream = pendingSubStream
                                        Log.w("TrackMap", "Deferred subtitle apply failed jellyfinIndex=${pendingSubStream.index} retry=$nextRetry")
                                    } else {
                                        pendingSubtitleSelectionRetryCount = 0
                                        Log.e("TrackMap", "Deferred subtitle apply aborted jellyfinIndex=${pendingSubStream.index} retriesExceeded=true")
                                    }
                                }
                            }
                        }
                        exoPlayer.logAudioTrackCatalog("onTracksChanged")
                    }
                }
            }

            // No redeclaration of playerView — using the outer one

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
                    if (exoPlayerRef === exoPlayer) exoPlayerRef = null
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
                        if (durationMs > 0) playbackDuration = durationMs / 1000
                    }
                    if (!isSeeking) {
                        playbackProgress = (exoPlayer.currentPosition + playbackOffsetMs) / 1000
                    }
                    val currentlyPlaying = exoPlayer.isPlaying
                    if (isPlaying != currentlyPlaying) isPlaying = currentlyPlaying
                    val pausedState = !currentlyPlaying
                    if (lastPausedState != pausedState) {
                        viewModel.updatePausedState(pausedState)
                        lastPausedState = pausedState
                    }
                    val bufferingState = exoPlayer.playbackState == Player.STATE_BUFFERING
                    if (isBuffering != bufferingState) isBuffering = bufferingState
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
                        pendingSubtitleSelectionStream = null
                        pendingSubtitleSelectionRetryCount = 0
                        player.configureTextTrackPreferences(stream = null, enabled = false)
                    }
                    else -> {
                        player.configureTextTrackPreferences(selectedSubtitleStream, enabled = true, subtitleStreams = subtitleStreams)
                        // Enqueue deferred selection for embedded subtitles (tracks may not be loaded yet)
                        if (!selectedSubtitleStream.isExternal) {
                            pendingSubtitleSelectionStream = selectedSubtitleStream
                            pendingSubtitleSelectionRetryCount = 0
                        }
                    }
                }
            }

            // FIX: renamed parameter from `playerView` to `pv` to avoid shadowing outer var
            fun updateExoDisplayMode(pv: PlayerView, mode: DisplayMode) {
                if (playerReleased) return
                when (mode) {
                    DisplayMode.FIT_SCREEN -> {
                        pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }
                    DisplayMode.CROP -> {
                        pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }
                    DisplayMode.STRETCH -> {
                        pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_DEFAULT
                    }
                    DisplayMode.ORIGINAL -> {
                        pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        exoPlayer.videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                    }
                }
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    // FIX: renamed chained `it` to `mod` to avoid shadowing
                    .let { mod ->
                        if (isScreenLocked) {
                            mod.pointerInput(Unit) {
                                detectTapGestures { /* consumed — block all touches when locked */ }
                            }
                        } else if (gesturesEnabled) mod.pointerInput(playerReleased) {
                            if (playerReleased) return@pointerInput
                            val screenWidth = size.width.toFloat()
                            val screenHeight = size.height.toFloat()
                            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
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
                                    startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                                    startBrightness = brightness
                                    isHorizontal = null
                                },
                                onDrag = { change, _ ->
                                    if (playerReleased) return@detectDragGestures
                                    val dragDelta = change.position - startOffset
                                    if (isHorizontal == null) {
                                        val absX = abs(dragDelta.x)
                                        val absY = abs(dragDelta.y)
                                        isHorizontal = if (absX > absY && absX > touchSlop) true
                                        else if (absY > absX && absY > touchSlop) false
                                        else {
                                            change.consume()
                                            return@detectDragGestures
                                        }
                                    }
                                    if (isHorizontal == true) {
                                        val delta = dragDelta.x / screenWidth * effectiveDuration
                                        val newPos = (startProgress + delta).coerceIn(0f, effectiveDuration.toFloat())
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
                                            gestureFeedback = GestureFeedback.Brightness((brightness * 100f).roundToInt())
                                        } else {
                                            val newV = (startVolume + factor * maxVolume).coerceIn(0f, maxVolume)
                                            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newV.roundToInt(), 0)
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
                                        viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
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
                        } else mod
                    }
                    .let { mod ->
                        if (isScreenLocked) mod
                        else mod.clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { showControls = !showControls }
                    }
                    .let { mod ->
                        if (isScreenLocked) mod
                        else if (gesturesEnabled) mod.pointerInput(playerReleased) {
                            if (playerReleased) return@pointerInput
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    if (playerReleased) return@detectTapGestures
                                    val half = size.width / 2
                                    if (offset.x < half) {
                                        exoPlayer.seekTo((exoPlayer.currentPosition - 10_000).coerceAtLeast(0))
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
                        } else mod
                    },
                // FIX: renamed factory lambda `it` param to `pv` to prevent shadowing
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
                    }.also { pv -> playerView = pv }
                },
                update = { view ->
                    if (!playerReleased) updateExoDisplayMode(view, displayMode)
                    view.subtitleView?.apply {
                        visibility = if (selectedSubtitleStream != null) View.VISIBLE else View.GONE
                        z = Float.MAX_VALUE
                    }
                }
            )

            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                VideoControlsOverlay(
                    mediaTitle = currentMediaItem.name,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    duration = effectiveDuration,
                    currentPosition = playbackProgress,
                    onPlayPauseClick = {
                        if (playerReleased) return@VideoControlsOverlay
                        if (exoPlayer.isPlaying) {
                            viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
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
                        viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
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
                            viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
                            viewModel.reportPlaybackStop(
                                mediaId = currentMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = playbackProgress,
                                isCompleted = playbackProgress >= effectiveDuration - 5
                            )
                            viewModel.playPreviousEpisode()
                        }
                    } else null,
                    onPlayNext = if (playerState.hasNextEpisode) {
                        {
                            viewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
                            viewModel.reportPlaybackStop(
                                mediaId = currentMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = playbackProgress,
                                isCompleted = playbackProgress >= effectiveDuration - 5
                            )
                            viewModel.playNextEpisode()
                        }
                    } else null,
                    speed = speed,
                    onSpeedChange = { newSpeed ->
                        if (playerReleased) return@VideoControlsOverlay
                        speed = newSpeed
                        exoPlayer.setPlaybackSpeed(newSpeed)
                        viewModel.updatePlaybackSpeed(newSpeed)
                    },
                    playbackQualityLabel = playerState.currentTranscodeQualityText,
                    onPlaybackQualityClick = if (canTranscode) { { showTranscodeDialog = true } } else null,
                    onAudioClick = if (audioStreams.isNotEmpty()) { { showAudioDialog = true } } else null,
                    onSubtitleClick = if (subtitleStreams.isNotEmpty()) { { showSubtitleDialog = true } } else null,
                    onDecoderClick = { showDecoderDialog = true },
                    onDisplayClick = { showDisplayDialog = true },
                    onPipClick = {
                        activity?.enterPictureInPictureMode(pipParams)
                        showControls = false
                    },
                    onLockClick = {
                        isScreenLocked = true
                        showControls = false
                    },
                    progressBarColor = progressBarColor,
                    seekProgressColor = seekProgressColor,
                    isSeeking = isSeeking
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

            if (isScreenLocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) { detectTapGestures { /* consume all touch events */ } }
                ) {
                    IconButton(
                        onClick = {
                            isScreenLocked = false
                            showControls = true
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(calculateRoundedValue(24).sdp)
                            .navigationBarsPadding()
                            .size(calculateRoundedValue(48).sdp)
                            .background(color = Color.White.copy(alpha = 0.15f), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = "Unlock screen",
                            tint = Color.White,
                            modifier = Modifier.size(calculateRoundedValue(28).sdp)
                        )
                    }
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
                        if (isLocalFile) {
                            exoPlayer.applyLocalAudioSelection(stream.index, stream, audioStreams)
                            exoPlayer.seekTo(currentPosition)
                            exoPlayer.playWhenReady = true
                        } else {
                            val applied = exoPlayer.configureAudioTrackPreferences(stream, audioStreams)
                            pendingAudioSelectionIndex = if (applied) null else stream.index
                            pendingAudioSelectionRetryCount = 0
                            // FIX: renamed to `audioBaseItem` to avoid shadowing
                            val audioBaseItem = viewModel.state.value.videoUrl?.let { url ->
                                ExoMediaItem.fromUri(url)
                            }
                            val subtitleStream = subtitleStreams.firstOrNull { s -> s.index == currentSubtitleTrack }
                            val resolvedAudioItem = if (subtitleStream?.isExternal == true) {
                                val config = buildSubtitleConfiguration(
                                    context = context,
                                    mediaItem = currentMediaItem,
                                    stream = subtitleStream,
                                    isLocalFile = false,
                                    serverUrl = serverUrl,
                                    accessToken = accessToken
                                )
                                audioBaseItem?.buildUpon()
                                    ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                    ?.build()
                            } else {
                                audioBaseItem
                            }
                            resolvedAudioItem?.let { item ->
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
                            // Use the proxied exoMediaItem (mTLS-aware) instead of raw videoUrl
                            val subBaseItem = viewModel.state.value.exoMediaItem
                                ?: viewModel.state.value.videoUrl?.let { url -> ExoMediaItem.fromUri(url) }
                            val finalItem = when {
                                stream == null -> {
                                    pendingSubtitleSelectionStream = null
                                    pendingSubtitleSelectionRetryCount = 0
                                    exoPlayer.configureTextTrackPreferences(stream = null, enabled = false)
                                    subBaseItem?.buildUpon()?.setSubtitleConfigurations(emptyList())?.build()
                                }
                                stream.isExternal -> {
                                    pendingSubtitleSelectionStream = null
                                    pendingSubtitleSelectionRetryCount = 0
                                    exoPlayer.configureTextTrackPreferences(stream, enabled = true, subtitleStreams = subtitleStreams)
                                    val config = buildSubtitleConfiguration(
                                        context = context,
                                        mediaItem = currentMediaItem,
                                        stream = stream,
                                        isLocalFile = false,
                                        serverUrl = serverUrl,
                                        accessToken = accessToken
                                    )
                                    subBaseItem?.buildUpon()
                                        ?.setSubtitleConfigurations(config?.let { listOf(it) } ?: emptyList())
                                        ?.build()
                                }
                                else -> {
                                    // Embedded subtitle: configure track preferences and enqueue deferred selection
                                    exoPlayer.configureTextTrackPreferences(stream, enabled = true, subtitleStreams = subtitleStreams)
                                    pendingSubtitleSelectionStream = stream
                                    pendingSubtitleSelectionRetryCount = 0
                                    subBaseItem
                                }
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
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Minsk)
            }
        }

        GestureFeedbackOverlay(feedback = gestureFeedback, modifier = Modifier.align(Alignment.Center))
    }
}

// ─── Private helpers ──────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
private fun createRenderersFactory(
    context: Context,
    decoderMode: DecoderMode,
    hdrFormatPreference: HdrFormatPreference
): DefaultRenderersFactory {
    val filteringMode = when (hdrFormatPreference) {
        HdrFormatPreference.HDR10_PLUS -> FfmpegFilteringMode.KEEP_HDR10_BASE
        HdrFormatPreference.DOLBY_VISION -> FfmpegFilteringMode.KEEP_DOLBY_VISION
        HdrFormatPreference.DOLBY_VISION_MEL -> FfmpegFilteringMode.KEEP_DOLBY_VISION_MEL
        else -> FfmpegFilteringMode.AUTO
    }
    return DefaultRenderersFactory(context).apply {
        experimentalSetHdrFilteringMode(filteringMode)
        when (decoderMode) {
            DecoderMode.HARDWARE_ONLY -> {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)
                setEnableDecoderFallback(false)
            }
            DecoderMode.SOFTWARE_ONLY -> {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                setEnableDecoderFallback(true)
            }
            DecoderMode.AUTO -> {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
                setEnableDecoderFallback(true)
            }
        }
    }
}

// FIX: removed unused findTrackOverride(trackType, targetIndex) single-param overload
// FIX: removed unused applyLocalAudioSelection(targetIndex: Int) single-param overload
// FIX: removed unused normalizeLanguageCode() extension function

@OptIn(UnstableApi::class)
private fun ExoPlayer.findTrackOverride(
    trackType: Int,
    targetIndex: Int,
    targetLanguage: String?
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
        } else null
    }
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.findTextTrackOverride(
    stream: MediaStream,
    subtitleStreams: List<MediaStream>
): TrackSelectionOverride? {
    data class TextCandidate(
        val ordinalIndex: Int,
        val trackIndex: Int,
        val group: Tracks.Group,
        val format: Format
    )

    val candidates = buildList {
        var ordinalIndex = 0
        currentTracks.groups.forEach { group ->
            if (group.type != C.TRACK_TYPE_TEXT) return@forEach
            for (trackIndex in 0 until group.length) {
                add(
                    TextCandidate(
                        ordinalIndex = ordinalIndex,
                        trackIndex = trackIndex,
                        group = group,
                        format = group.getTrackFormat(trackIndex)
                    )
                )
                ordinalIndex++
            }
        }
    }
    if (candidates.isEmpty()) return null

    // Strategy 1: format.id → Jellyfin index direct match (validated 1:1 mapping)
    val candidateIdInts = candidates.mapNotNull { c -> c.format.id?.toIntOrNull() }
    val canUseFormatIdMapping = run {
        if (candidateIdInts.size != candidates.size || candidates.size != subtitleStreams.size || subtitleStreams.isEmpty()) {
            false
        } else {
            val streamIds = subtitleStreams.map { it.index }
            candidateIdInts.distinct().size == candidateIdInts.size &&
                    streamIds.distinct().size == streamIds.size &&
                    candidateIdInts.toSet() == streamIds.toSet()
        }
    }

    if (canUseFormatIdMapping) {
        candidates.firstOrNull { c -> c.format.id?.toIntOrNull() == stream.index }
            ?.let { matched ->
                Log.i("TrackMap", "findTextTrackOverride: format.id match jellyfinIndex=${stream.index} exoId=${matched.format.id}")
                return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
            }
    }

    // Strategy 2: constant-offset mapping between ExoPlayer format.ids and Jellyfin indices
    if (candidateIdInts.size == candidates.size && subtitleStreams.isNotEmpty()) {
        val streamIds = subtitleStreams.map { it.index }
        if (candidateIdInts.size == streamIds.size) {
            val sortedCandidateIds = candidateIdInts.sorted()
            val sortedStreamIds = streamIds.sorted()
            val offsets = sortedCandidateIds.zip(sortedStreamIds) { exoId, jellyfinId -> exoId - jellyfinId }
            val constantOffset = offsets.distinct().singleOrNull()
            if (constantOffset != null) {
                candidates.firstOrNull { c -> c.format.id?.toIntOrNull() == stream.index + constantOffset }
                    ?.let { matched ->
                        Log.i("TrackMap", "findTextTrackOverride: offset match jellyfinIndex=${stream.index} offset=$constantOffset exoId=${matched.format.id}")
                        return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
                    }
            }
        }
    }

    // Strategy 3: semantic match by language + codec
    val streamLang = normalizeLanguageForTextMatch(stream.language)
    val streamCodec = stream.codec?.trim()?.lowercase(Locale.ROOT)
    candidates.firstOrNull { c ->
        val formatLang = normalizeLanguageForTextMatch(c.format.language)
        val langMatch = !streamLang.isNullOrBlank() && !formatLang.isNullOrBlank() && streamLang == formatLang
        val codecMatch = streamCodec != null && c.format.sampleMimeType != null &&
                stream.toSubtitleMimeType() == c.format.sampleMimeType
        langMatch && codecMatch
    }?.let { matched ->
        Log.i("TrackMap", "findTextTrackOverride: semantic match (lang+codec) jellyfinIndex=${stream.index} lang=${stream.language} codec=${stream.codec}")
        return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
    }

    // Strategy 4: language-only match (pick first track with matching language)
    if (!streamLang.isNullOrBlank()) {
        candidates.firstOrNull { c ->
            val formatLang = normalizeLanguageForTextMatch(c.format.language)
            !formatLang.isNullOrBlank() && streamLang == formatLang
        }?.let { matched ->
            Log.i("TrackMap", "findTextTrackOverride: language match jellyfinIndex=${stream.index} lang=${stream.language}")
            return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
        }
    }

    // Strategy 5: ordinal position fallback — match the stream's position within the Jellyfin subtitle list
    val streamPosition = subtitleStreams.indexOfFirst { it.index == stream.index }
    if (streamPosition in 0 until candidates.size) {
        val matched = candidates[streamPosition]
        Log.i("TrackMap", "findTextTrackOverride: ordinal fallback jellyfinIndex=${stream.index} position=$streamPosition")
        return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
    }

    Log.w("TrackMap", "findTextTrackOverride: no match for jellyfinIndex=${stream.index} lang=${stream.language} exoCandidates=${candidates.size} jellyfinStreams=${subtitleStreams.size}")
    return null
}

private fun normalizeLanguageForTextMatch(value: String?): String? {
    val normalized = value?.trim()?.lowercase(Locale.ROOT)?.replace('_', '-') ?: return null
    return when (normalized) {
        "eng", "english" -> "en"
        "jpn", "japanese" -> "ja"
        "fre", "fra", "french" -> "fr"
        "spa", "spanish" -> "es"
        "ger", "deu", "german" -> "de"
        "por", "portuguese" -> "pt"
        "ita", "italian" -> "it"
        "rus", "russian" -> "ru"
        "kor", "korean" -> "ko"
        "chi", "zho", "chinese" -> "zh"
        "ara", "arabic" -> "ar"
        "hin", "hindi" -> "hi"
        "tha", "thai" -> "th"
        "vie", "vietnamese" -> "vi"
        "pol", "polish" -> "pl"
        "dut", "nld", "dutch" -> "nl"
        "swe", "swedish" -> "sv"
        "nor", "nob", "nno", "norwegian" -> "no"
        "dan", "danish" -> "da"
        "fin", "finnish" -> "fi"
        "tur", "turkish" -> "tr"
        "heb", "hebrew" -> "he"
        "hun", "hungarian" -> "hu"
        "ces", "cze", "czech" -> "cs"
        "ron", "rum", "romanian" -> "ro"
        "bul", "bulgarian" -> "bg"
        "hrv", "croatian" -> "hr"
        "slk", "slo", "slovak" -> "sk"
        "ukr", "ukrainian" -> "uk"
        "ind", "indonesian" -> "id"
        "msa", "may", "malay" -> "ms"
        else -> normalized
    }
}

@OptIn(UnstableApi::class)
private fun MediaStream.toSubtitleMimeType(): String? {
    val codecValue = codec?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return when {
        codecValue == "srt" || codecValue == "subrip" -> MimeTypes.APPLICATION_SUBRIP
        codecValue == "ass" || codecValue == "ssa" -> MimeTypes.TEXT_SSA
        codecValue == "webvtt" || codecValue == "wvtt" || codecValue == "vtt" -> MimeTypes.TEXT_VTT
        codecValue == "pgs" || codecValue == "hdmv_pgs_subtitle" || codecValue == "pgssub" -> MimeTypes.APPLICATION_PGS
        codecValue == "dvbsub" || codecValue == "dvb_subtitle" || codecValue == "dvb" -> MimeTypes.APPLICATION_DVBSUBS
        codecValue == "mov_text" || codecValue == "tx3g" || codecValue == "mp4vtt" -> MimeTypes.APPLICATION_MP4VTT
        codecValue == "ttml" || codecValue == "dfxp" -> MimeTypes.APPLICATION_TTML
        codecValue == "eia608" || codecValue == "cea608" || codecValue == "cc_dec" -> MimeTypes.APPLICATION_CEA608
        codecValue == "eia708" || codecValue == "cea708" -> MimeTypes.APPLICATION_CEA708
        else -> null
    }
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.applyLocalAudioSelection(
    targetIndex: Int,
    targetStream: MediaStream? = null,
    audioStreams: List<MediaStream> = emptyList()
): Boolean {
    val builder = trackSelectionParameters.buildUpon()
        .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
        .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)

    val resolvedStream = targetStream ?: audioStreams.firstOrNull { it.index == targetIndex }
    logJellyfinAudioCatalog(
        reason = "applyLocalAudioSelection",
        requestedJellyfinIndex = targetIndex,
        audioStreams = audioStreams,
        videoUrl = currentMediaItem?.localConfiguration?.uri?.toString()
    )
    logAudioTrackCatalog("applyLocalAudioSelection before resolve")
    val override = when {
        resolvedStream != null && audioStreams.isNotEmpty() ->
            findAudioTrackOverride(resolvedStream, audioStreams, allowOrdinalFallback = true)
        else -> findTrackOverride(C.TRACK_TYPE_AUDIO, targetIndex, resolvedStream?.preferredLanguage())
    }

    if (override != null) {
        Log.i("TrackMap", "applyLocalAudioSelection override=$override jellyfinIndex=$targetIndex")
        builder.addOverride(override)
        trackSelectionParameters = builder.build()
        logAudioTrackCatalog("applyLocalAudioSelection after apply")
        return true
    } else {
        Log.w("TrackMap", "applyLocalAudioSelection failed for jellyfinIndex=$targetIndex")
        resolvedStream?.preferredLanguage()?.let { builder.setPreferredAudioLanguage(it) }
    }
    trackSelectionParameters = builder.build()
    logAudioTrackCatalog("applyLocalAudioSelection after apply")
    return false
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.findAudioTrackOverride(
    stream: MediaStream,
    streams: List<MediaStream>,
    allowOrdinalFallback: Boolean = true
): TrackSelectionOverride? {
    data class AudioCandidate(
        val ordinalIndex: Int,
        val trackIndex: Int,
        val group: Tracks.Group,
        val format: Format,
        val isSupported: Boolean
    )

    val candidates = buildList {
        var ordinalIndex = 0
        currentTracks.groups.forEach { group ->
            if (group.type != C.TRACK_TYPE_AUDIO) return@forEach
            for (trackIndex in 0 until group.length) {
                add(
                    AudioCandidate(
                        ordinalIndex = ordinalIndex,
                        trackIndex = trackIndex,
                        group = group,
                        format = group.getTrackFormat(trackIndex),
                        isSupported = group.isTrackSupported(trackIndex)
                    )
                )
                ordinalIndex++
            }
        }
    }
    if (candidates.isEmpty()) return null

    val candidateIdInts = candidates.mapNotNull { candidate -> candidate.format.id?.toIntOrNull() }

    val canUseFormatIdMapping = run {
        if (candidateIdInts.size != candidates.size || candidates.size != streams.size || streams.isEmpty()) {
            false
        } else {
            val streamIds = streams.map { it.index }
            candidateIdInts.distinct().size == candidateIdInts.size &&
                    streamIds.distinct().size == streamIds.size &&
                    candidateIdInts.toSet() == streamIds.toSet()
        }
    }

    if (canUseFormatIdMapping) {
        candidates.firstOrNull { candidate ->
            candidate.format.id?.toIntOrNull() == stream.index
        }?.let { matched ->
            return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
        }
    } else {
        Log.w("TrackMap", "Skipping format.id audio mapping; ids are not aligned with jellyfin indices (exoCandidates=${candidates.size}, jellyfinStreams=${streams.size})")
        if (candidateIdInts.size == candidates.size && streams.isNotEmpty()) {
            val streamIds = streams.map { it.index }
            if (candidateIdInts.size == streamIds.size) {
                val sortedCandidateIds = candidateIdInts.sorted()
                val sortedStreamIds = streamIds.sorted()
                val offsets = sortedCandidateIds.zip(sortedStreamIds) { exoId, jellyfinId -> exoId - jellyfinId }
                val constantOffset = offsets.distinct().singleOrNull()
                if (constantOffset != null) {
                    candidates.firstOrNull { candidate ->
                        candidate.format.id?.toIntOrNull() == stream.index + constantOffset
                    }?.let { matched ->
                        return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
                    }
                }
            }
        }
    }

    val semanticScores = candidates.map { candidate -> candidate to scoreAudioCandidate(stream, candidate.format) }
    val bestScore = semanticScores.maxOfOrNull { it.second } ?: 0
    if (bestScore >= 9) {
        val bestCandidates = semanticScores.filter { it.second == bestScore }
        val matched = bestCandidates.first().first
        return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
    }

    if (allowOrdinalFallback) {
        val streamPosition = streams.indexOfFirst { it.index == stream.index }
        candidates.firstOrNull { candidate ->
            streamPosition in 0 until candidates.size && candidate.ordinalIndex == streamPosition
        }?.let { matched ->
            return TrackSelectionOverride(matched.group.mediaTrackGroup, matched.trackIndex)
        }
    }
    return null
}

private fun MediaStream.preferredLanguage(): String? =
    sequenceOf(language, displayLanguage).mapNotNull { it?.trim() }.firstOrNull { it.isNotEmpty() }

@OptIn(UnstableApi::class)
private fun extractQueryParamCaseInsensitive(url: String?, paramName: String): String? {
    if (url.isNullOrBlank()) return null
    return runCatching {
        val uri = url.toUri()
        uri.getQueryParameter(paramName)
            ?: uri.queryParameterNames
                .firstOrNull { it.equals(paramName, ignoreCase = true) }
                ?.let(uri::getQueryParameter)
    }.getOrNull()
}

private fun normalizeAudioTokens(value: String?): Set<String> {
    if (value.isNullOrBlank()) return emptySet()
    return value.lowercase(Locale.ROOT)
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
        .split(' ')
        .filter { token ->
            token.length > 1 &&
                    token !in setOf("dolby", "digital", "audio", "english", "eng", "track", "default")
        }
        .toSet()
}

private fun normalizeLanguageForAudioMatch(value: String?): String? {
    val normalized = value?.trim()?.lowercase(Locale.ROOT)?.replace('_', '-') ?: return null
    return when (normalized) {
        "eng", "english" -> "en"
        "jpn", "japanese" -> "ja"
        "fre", "fra", "french" -> "fr"
        "spa", "spanish" -> "es"
        else -> normalized
    }
}

private fun scoreAudioCandidate(stream: MediaStream, format: Format): Int {
    var score = 0
    val streamMime = stream.toAudioMimeType()
    val formatMime = format.sampleMimeType
    if (streamMime != null && formatMime != null && streamMime == formatMime) score += 8

    val streamChannels = stream.channels
    val formatChannels = format.channelCount
    if (streamChannels != null && formatChannels != Format.NO_VALUE && streamChannels == formatChannels) score += 4

    val streamLanguages = sequenceOf(stream.language, stream.displayLanguage)
        .mapNotNull(::normalizeLanguageForAudioMatch).toSet()
    val formatLang = normalizeLanguageForAudioMatch(format.language)
    if (formatLang != null && streamLanguages.contains(formatLang)) score += 2

    val streamTitle = stream.displayTitle ?: stream.title
    val streamTokens = normalizeAudioTokens(streamTitle)
    val formatTokens = normalizeAudioTokens(format.label)
    if (streamTokens.isNotEmpty() && formatTokens.isNotEmpty()) {
        val overlap = streamTokens.intersect(formatTokens).size
        score += when {
            overlap >= 4 -> 8
            overlap >= 2 -> 5
            overlap >= 1 -> 2
            else -> 0
        }
    }
    return score
}

@OptIn(UnstableApi::class)
private fun logJellyfinAudioCatalog(
    reason: String,
    requestedJellyfinIndex: Int?,
    audioStreams: List<MediaStream>,
    videoUrl: String?
) {
    val urlAudioIndex = extractQueryParamCaseInsensitive(videoUrl, "AudioStreamIndex")
    val streamsSummary = audioStreams.joinToString(" || ") { s ->
        "jf=${s.index} default=${s.isDefault} codec=${s.codec} lang=${s.language ?: s.displayLanguage} title=${s.displayTitle ?: s.title} ch=${s.channels ?: s.channelLayout}"
    }
    Log.i("TrackMap", "$reason requestedJellyfinIndex=$requestedJellyfinIndex urlAudioIndex=$urlAudioIndex streamCount=${audioStreams.size} streams=[$streamsSummary]")
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.logAudioTrackCatalog(reason: String) {
    val audioGroups = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
    if (audioGroups.isEmpty()) {
        Log.w("TrackMap", "$reason exoAudioGroups=0")
        return
    }
    val rows = buildList {
        var ordinal = 0
        audioGroups.forEachIndexed { groupIndex, group ->
            for (trackIndex in 0 until group.length) {
                val format = group.getTrackFormat(trackIndex)
                add("ord=$ordinal group=$groupIndex trackIndex=$trackIndex selected=${group.isTrackSelected(trackIndex)} supported=${group.isTrackSupported(trackIndex)} id=${format.id} lang=${format.language} label=${format.label} mime=${format.sampleMimeType} ch=${format.channelCount} br=${format.bitrate}")
                ordinal++
            }
        }
    }
    Log.i("TrackMap", "$reason exoAudioTrackCount=${rows.size}\n${rows.joinToString(separator = "\n")}")
}

private fun MediaStream.toAudioMimeType(): String? {
    val codecValue = codec?.trim()?.lowercase(Locale.ROOT).orEmpty()
    return when {
        codecValue == "ac3" -> MimeTypes.AUDIO_AC3
        codecValue == "eac3" -> MimeTypes.AUDIO_E_AC3
        // Codec identifiers — @Suppress used to silence spell-check on technical terms
        @Suppress("SpellCheckingInspection")
        (codecValue == "truehd" || codecValue == "mlp") -> MimeTypes.AUDIO_TRUEHD
        codecValue == "dca" || codecValue == "dts" -> MimeTypes.AUDIO_DTS
        @Suppress("SpellCheckingInspection")
        (codecValue == "dts-hd" || codecValue == "dtshd") -> MimeTypes.AUDIO_DTS_HD
        codecValue == "aac" || codecValue.startsWith("mp4a") -> MimeTypes.AUDIO_AAC
        codecValue == "mp3" -> MimeTypes.AUDIO_MPEG
        codecValue == "flac" -> MimeTypes.AUDIO_FLAC
        codecValue == "opus" -> MimeTypes.AUDIO_OPUS
        codecValue == "vorbis" -> MimeTypes.AUDIO_VORBIS
        @Suppress("SpellCheckingInspection")
        (codecValue == "pcm" || codecValue == "lpcm" || codecValue == "pcm_s16le") -> MimeTypes.AUDIO_RAW
        else -> null
    }
}

@OptIn(UnstableApi::class)
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
        else -> localFile?.toUri() ?: mediaItem.getSubtitleUrl(serverUrl, accessToken, stream.index).toUri()
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

@OptIn(UnstableApi::class)
private fun ExoPlayer.configureTextTrackPreferences(
    stream: MediaStream?,
    enabled: Boolean,
    subtitleStreams: List<MediaStream> = emptyList()
) {
    val builder = trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
    if (enabled && stream != null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        if (stream.isExternal) {
            stream.language?.takeUnless { it.isBlank() }?.let { language ->
                builder.setPreferredTextLanguage(language)
            }
        } else {
            val override = if (subtitleStreams.isNotEmpty()) {
                findTextTrackOverride(stream, subtitleStreams)
            } else {
                findTrackOverride(C.TRACK_TYPE_TEXT, stream.index, stream.language?.takeUnless { it.isBlank() })
            }
            if (override != null) {
                builder.addOverride(override)
            }
            // Always set language preference as secondary signal
            stream.language?.takeUnless { it.isBlank() }?.let { language ->
                builder.setPreferredTextLanguage(language)
            }
        }
    } else {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
    }
    trackSelectionParameters = builder.build()
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.configureAudioTrackPreferences(
    stream: MediaStream?,
    audioStreams: List<MediaStream>
): Boolean {
    val builder = trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO)
    var applied = false
    if (stream != null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
        logJellyfinAudioCatalog(
            reason = "configureAudioTrackPreferences",
            requestedJellyfinIndex = stream.index,
            audioStreams = audioStreams,
            videoUrl = currentMediaItem?.localConfiguration?.uri?.toString()
        )
        logAudioTrackCatalog("configureAudioTrackPreferences before resolve")
        val override = findAudioTrackOverride(stream = stream, streams = audioStreams, allowOrdinalFallback = true)
        if (override != null) {
            builder.addOverride(override)
            applied = true
            Log.i("TrackMap", "configureAudioTrackPreferences override=$override jellyfinIndex=${stream.index}")
        } else {
            Log.w("TrackMap", "configureAudioTrackPreferences failed for jellyfinIndex=${stream.index}; keeping server-selected track")
        }
    }
    trackSelectionParameters = builder.build()
    logAudioTrackCatalog("configureAudioTrackPreferences after apply applied=$applied")
    return applied
}

@OptIn(UnstableApi::class)
private fun ExoPlayer.applyLocalSubtitleSelection(
    targetIndex: Int?,
    subtitleStreams: List<MediaStream>,
    mediaItem: MediaItem,
    context: Context,
    serverUrl: String,
    accessToken: String,
    isLocalFile: Boolean
): ExoMediaItem? {
    val builder = trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_TEXT)
    if (targetIndex == null) {
        builder.setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
        trackSelectionParameters = builder.build()
        val currentItem = currentMediaItem
        val hasConfigs = currentItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true
        return if (hasConfigs) currentItem.buildUpon().setSubtitleConfigurations(emptyList()).build() else null
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
        val override = if (subtitleStreams.isNotEmpty()) {
            findTextTrackOverride(stream, subtitleStreams)
        } else {
            findTrackOverride(C.TRACK_TYPE_TEXT, targetIndex, stream.language?.takeUnless { it.isBlank() })
        }
        if (override != null) builder.addOverride(override)
        // Always set language preference as secondary signal
        stream.language?.takeUnless { it.isBlank() }?.let { language ->
            builder.setPreferredTextLanguage(language)
        }
        trackSelectionParameters = builder.build()
        val currentItem = currentMediaItem
        val hasConfigs = currentItem?.localConfiguration?.subtitleConfigurations?.isNotEmpty() == true
        if (hasConfigs) currentItem.buildUpon().setSubtitleConfigurations(emptyList()).build() else null
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
    // FIX: renamed `it` to `dir` to avoid shadowing
    val external = externalDir?.let { dir -> File(dir, "downloads/subtitles/$fileName") }
    return external?.takeIf { it.exists() }
}