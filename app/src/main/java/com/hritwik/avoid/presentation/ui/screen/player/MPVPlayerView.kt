package com.hritwik.avoid.presentation.ui.screen.player

import android.app.PictureInPictureParams
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import android.util.Rational
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.data.network.LocalNetworkSslHelper
import com.hritwik.avoid.di.PlayerNetworkEntryPoint
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
import com.hritwik.avoid.presentation.ui.state.VideoPlaybackState
import com.hritwik.avoid.presentation.ui.theme.Minsk
import com.hritwik.avoid.presentation.viewmodel.player.VideoPlaybackViewModel
import com.hritwik.avoid.presentation.viewmodel.user.UserDataViewModel
import com.hritwik.avoid.utils.RuntimeConfig
import com.hritwik.avoid.utils.constants.ApiConstants
import com.hritwik.avoid.utils.constants.MpvConstants
import com.hritwik.avoid.utils.constants.PreferenceConstants.SKIP_PROMPT_FLOATING_DURATION_MS
import com.hritwik.avoid.utils.extensions.getSubtitleUrl
import com.hritwik.avoid.utils.extensions.toSubtitleFileExtension
import com.hritwik.avoid.utils.helpers.calculateRoundedValue
import com.hritwik.avoid.utils.helpers.hideNavigationButtons
import dagger.hilt.android.EntryPointAccessors
import dev.marcelsoftware.mpvcompose.DefaultLogObserver
import dev.marcelsoftware.mpvcompose.MPVLib
import dev.marcelsoftware.mpvcompose.MPVPlayer
import ir.kaaveh.sdpcompose.sdp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.roundToLong

@OptIn(UnstableApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MpvPlayerView(
    mediaItem: MediaItem,
    playerState: VideoPlaybackState,
    decoderMode: DecoderMode,
    displayMode: DisplayMode,
    userId: String,
    accessToken: String,
    serverUrl: String,
    autoPlayNextEpisode: Boolean,
    autoSkipSegments: Boolean,
    gesturesEnabled: Boolean,
    onBackClick: () -> Unit,
    videoPlaybackViewModel: VideoPlaybackViewModel,
    userDataViewModel: UserDataViewModel
) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    var mpvConfigFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(context) {
        val ensuredFile = withContext(Dispatchers.IO) { ensureMpvConfigFile(context) }
        mpvConfigFile = ensuredFile
    }
    val currentMediaItem = playerState.mediaItem ?: mediaItem
    val latestMediaItem by rememberUpdatedState(currentMediaItem)
    val hasNextEpisode by rememberUpdatedState(playerState.hasNextEpisode)
    val autoPlayNext by rememberUpdatedState(autoPlayNextEpisode)
    val lifecycleOwner = LocalLifecycleOwner.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioFocusRequest = rememberAudioFocusRequest(audioManager)
    val scope = rememberCoroutineScope()
    val appContext = remember(context.applicationContext) { context.applicationContext }
    val mtlsProvider = remember(appContext) {
        EntryPointAccessors.fromApplication(
            appContext,
            PlayerNetworkEntryPoint::class.java
        ).mtlsCertificateProvider()
    }
    val sslConfig = remember(mtlsProvider) {
        LocalNetworkSslHelper.createSslConfig(mtlsProvider.keyManager())
    }
    val subtitleHttpClient = remember(sslConfig) {
        OkHttpClient.Builder()
            .sslSocketFactory(sslConfig.sslSocketFactory, sslConfig.trustManager)
            .hostnameVerifier(sslConfig.hostnameVerifier)
            .build()
    }
    val externalSubtitleCache = remember { mutableMapOf<Int, File>() }
    var externalSubtitleLoadJob by remember { mutableStateOf<Job?>(null) }
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val screenAspectRatio = remember {
        val metrics = context.resources.displayMetrics
        metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat()
    }
    var showControls by remember { mutableStateOf(true) }
    LaunchedEffect(activity) {
        activity?.hideNavigationButtons()
    }
    LaunchedEffect(showControls) {
        activity?.hideNavigationButtons()
    }
    var isPlaying by remember { mutableStateOf(true) }
    var resumeOnStart by remember { mutableStateOf(false) }
    var playbackDuration by remember { mutableLongStateOf(1L) }
    val overrideDurationSeconds = playerState.totalDurationSeconds?.takeIf { it > 0 }
    val effectiveDuration by rememberUpdatedState(overrideDurationSeconds ?: playbackDuration)
    var playbackProgress by remember { mutableLongStateOf(playerState.startPositionMs / 1000) }
    var volume by remember { mutableLongStateOf(playerState.volume) }
    var speed by remember { mutableFloatStateOf(playerState.playbackSpeed) }
    var isBuffering by remember { mutableStateOf(false) }
    var isSeeking by remember { mutableStateOf(false) }
    var isMpvInitialized by remember { mutableStateOf(false) }
    val window = activity?.window
    var brightness by remember {
        mutableFloatStateOf(window?.attributes?.screenBrightness?.takeIf { it >= 0 } ?: 0.5f)
    }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showSubtitleDialog by remember { mutableStateOf(false) }
    var showDecoderDialog by remember { mutableStateOf(false) }
    var showDisplayDialog by remember { mutableStateOf(false) }
    var showTranscodeDialog by remember { mutableStateOf(false) }

    val audioStreams = playerState.availableAudioStreams
    val audioIndexToMpvTrackIds = remember(audioStreams) {
        audioStreams
            .sortedBy(MediaStream::index)
            .mapIndexed { mpvIndex, stream -> stream.index to mpvIndex + 1 }
            .toMap()
    }
    val subtitleStreams = playerState.availableSubtitleStreams
    val subtitleIndexToMpvTrackIds = remember(subtitleStreams) {
        subtitleStreams
            .filterNot(MediaStream::isExternal)
            .sortedBy(MediaStream::index)
            .mapIndexed { mpvIndex, stream -> stream.index to mpvIndex + 1 }
            .toMap()
    }
    val currentAudioTrack = playerState.audioStreamIndex
    val currentSubtitleTrack = playerState.subtitleStreamIndex

    var gestureFeedback by remember { mutableStateOf<GestureFeedback?>(null) }

    var skipLabel by remember { mutableStateOf("") }
    var currentSkipSegmentId by remember { mutableStateOf<String?>(null) }
    var skipSegmentEndMs by remember { mutableLongStateOf(0L) }
    var showOverlaySkipButton by remember { mutableStateOf(false) }
    var showFloatingSkipButton by remember { mutableStateOf(false) }
    var skipPromptDeadlineMs by remember { mutableLongStateOf(0L) }
    val initialSubtitleLanguage = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack }?.language
        ?: playerState.preferredSubtitleLanguage

    LaunchedEffect(playerState.totalDurationSeconds) {
        val overrideSeconds = playerState.totalDurationSeconds
        if (overrideSeconds != null && overrideSeconds > 0) {
            playbackDuration = overrideSeconds
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
                videoPlaybackViewModel.skipSegment()
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

    DisposableEffect(lifecycleOwner, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && activity?.isInPictureInPictureMode == true) {
                showControls = false
            }
            if (event == Lifecycle.Event.ON_RESUME && activity?.isInPictureInPictureMode == false) {
                activity.hideNavigationButtons()
                showControls = true
            }
            if ((event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) && activity?.isInPictureInPictureMode == false) {
                resumeOnStart = isPlaying
                MPVLib.setPropertyBoolean("pause", true)
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
                isPlaying = false
            }
            if ((event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) && activity?.isInPictureInPictureMode == false) {
                if (resumeOnStart) {
                    val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
                    if (focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        MPVLib.setPropertyBoolean("pause", false)
                        isPlaying = true
                    }
                    resumeOnStart = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isRemoteSource = playerState.videoUrl?.let { it.toUri().scheme != "file" } ?: false
    val canTranscode = isRemoteSource && playerState.playbackTranscodeOptions.size > 1

    var pendingTrackChangeEvent by remember { mutableStateOf<TrackChangeEvent?>(null) }

    fun getSubtitleFont(language: String?): String =
        when (language?.take(2)?.lowercase()) {
            "ar" -> "Noto Sans Arabic"
            "hi" -> "Noto Sans Devanagari"
            else -> "Noto Sans"
        }

    fun disableMpvSubtitles() {
        MPVLib.setPropertyString("sid", "no")
        MPVLib.setPropertyBoolean("sub-visibility", false)
        MPVLib.command(arrayOf("sub-remove", "all"))
    }

    fun applyMpvSubtitleSelection(subtitleIndex: Int?) {
        externalSubtitleLoadJob?.cancel()
        val language = subtitleStreams.firstOrNull { it.index == subtitleIndex }?.language
            ?: playerState.preferredSubtitleLanguage
        MPVLib.setPropertyString("sub-font", getSubtitleFont(language))

        if (subtitleIndex == null) {
            disableMpvSubtitles()
            return
        }

        val stream = subtitleStreams.firstOrNull { it.index == subtitleIndex }
        if (stream?.isExternal == true) {
            MPVLib.command(arrayOf("sub-remove", "all"))
            val targetSubtitleIndex = subtitleIndex
            externalSubtitleLoadJob = scope.launch {
                val file = ensureExternalSubtitleFile(
                    context = context,
                    mediaItem = currentMediaItem,
                    stream = stream,
                    serverUrl = serverUrl,
                    accessToken = accessToken,
                    client = subtitleHttpClient,
                    cache = externalSubtitleCache
                )
                if (!isActive) return@launch

                val filePath = file?.path
                    ?: currentMediaItem.getSubtitleUrl(serverUrl, accessToken, targetSubtitleIndex)
                try {
                    MPVLib.command(arrayOf("sub-add", filePath, "select"))
                    MPVLib.setPropertyBoolean("sub-visibility", true)
                } catch (error: Exception) {
                    Log.e("MpvPlayerView", "Failed to load external subtitle $targetSubtitleIndex", error)
                    disableMpvSubtitles()
                }
            }
        } else {
            MPVLib.command(arrayOf("sub-remove", "all"))
            val mpvSubtitleIndex = subtitleIndexToMpvTrackIds[subtitleIndex]
            if (mpvSubtitleIndex != null) {
                MPVLib.setPropertyString("sid", mpvSubtitleIndex.toString())
                MPVLib.setPropertyBoolean("sub-visibility", true)
            } else {
                disableMpvSubtitles()
            }
        }
    }

    LaunchedEffect(Unit) {
        videoPlaybackViewModel.trackChangeEvents.collect { event ->
            pendingTrackChangeEvent = event
        }
    }

    val handleAudioTrackChange: (Int) -> Unit = { newAudioIndex ->
        videoPlaybackViewModel.updateAudioStream(
            newAudioIndex,
            currentMediaItem,
            serverUrl,
            accessToken,
            shouldRebuildUrl = isRemoteSource
        )
        audioIndexToMpvTrackIds[newAudioIndex]?.let { mpvAudioId ->
            MPVLib.setPropertyString("aid", mpvAudioId.toString())
        }
    }

    val handleSubtitleTrackChange: (Int?) -> Unit = { newSubtitleIndex ->
        videoPlaybackViewModel.updateSubtitleStream(
            newSubtitleIndex,
            currentMediaItem,
            serverUrl,
            accessToken,
            shouldRebuildUrl = isRemoteSource
        )
        applyMpvSubtitleSelection(newSubtitleIndex)
    }
    val handleDisplayModeChange: (DisplayMode) -> Unit = { newDisplayMode ->
        userDataViewModel.setDisplayMode(newDisplayMode)
        when (newDisplayMode) {
            DisplayMode.FIT_SCREEN -> {
                MPVLib.setPropertyString("panscan", "0")
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            DisplayMode.CROP -> {
                MPVLib.setPropertyString("panscan", "1")
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
            DisplayMode.STRETCH -> {
                MPVLib.setPropertyString("panscan", "0")
                MPVLib.setPropertyString("video-unscaled", "no")
                MPVLib.setPropertyString("video-aspect-override", screenAspectRatio.toString())
            }
            DisplayMode.ORIGINAL -> {
                MPVLib.setPropertyString("panscan", "0")
                MPVLib.setPropertyString("video-unscaled", "yes")
                MPVLib.setPropertyString("video-aspect-override", "no")
            }
        }
    }

    val resolvedVideoUrl = remember(playerState.videoUrl) {
        playerState.videoUrl?.let { url ->
            val uri = url.toUri()
            if (uri.scheme == "file") uri.path else url
        }
    }

    val windowInsetsController = remember(activity) {
        activity?.window?.let { WindowCompat.getInsetsController(it, it.decorView) }
    }

    LaunchedEffect(gestureFeedback) {
        if (gestureFeedback != null) {
            delay(1000)
            gestureFeedback = null
        }
    }

    DisposableEffect(audioManager) {
        audioManager.requestAudioFocus(audioFocusRequest)
        onDispose { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
    }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
            val focusResult = audioManager.requestAudioFocus(audioFocusRequest)
            if (focusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                MPVLib.setPropertyBoolean("pause", true)
                isPlaying = false
            }
        } else {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        }
    }

    LaunchedEffect(showControls) {
        if (showControls) {
            windowInsetsController?.show(WindowInsetsCompat.Type.statusBars())
            windowInsetsController?.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            windowInsetsController?.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            windowInsetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
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
            if (!isSeeking && playbackProgress > 0) {
                videoPlaybackViewModel.savePlaybackPosition(
                    mediaId = currentMediaItem.id,
                    userId = userId,
                    accessToken = accessToken,
                    positionSeconds = playbackProgress
                )
            }
            delay(1000L)
        }
    }

    LaunchedEffect(resolvedVideoUrl, isMpvInitialized) {
        if (!isMpvInitialized) return@LaunchedEffect

        resolvedVideoUrl?.let {
            MPVLib.setOptionString("start", playbackProgress.toString())
            MPVLib.command(arrayOf("loadfile", it, "replace"))
            MPVLib.command(arrayOf("seek", playbackProgress.toString(), "absolute", "exact"))
            MPVLib.setPropertyBoolean("pause", false)
        }

        currentAudioTrack?.let { audioIndex ->
            audioIndexToMpvTrackIds[audioIndex]?.let { mpvAudioId ->
                MPVLib.setPropertyString("aid", mpvAudioId.toString())
            }
        }
        applyMpvSubtitleSelection(currentSubtitleTrack)
    }

    LaunchedEffect(pendingTrackChangeEvent, isMpvInitialized, playerState.videoUrl) {
        val event = pendingTrackChangeEvent
        if (!isMpvInitialized || event == null) return@LaunchedEffect

        if (isRemoteSource) {
            val parsedUrl = playerState.videoUrl.let(Uri::parse) ?: return@LaunchedEffect
            val targetAudio = event.audioIndex?.toString()
            val targetSubtitle = event.subtitleIndex?.toString()
            val urlAudio = parsedUrl.getQueryParameter("AudioStreamIndex")
            val urlSubtitle = parsedUrl.getQueryParameter("SubtitleStreamIndex")

            if (targetAudio != null && urlAudio != targetAudio) {
                return@LaunchedEffect
            }
            if (targetAudio == null && urlAudio != null) {
                return@LaunchedEffect
            }
            if (targetSubtitle != null && urlSubtitle != targetSubtitle) {
                return@LaunchedEffect
            }
            if (event.subtitleIndex == null && urlSubtitle != null) {
                return@LaunchedEffect
            }
        }

        applyMpvSubtitleSelection(event.subtitleIndex)
        pendingTrackChangeEvent = null
    }

    BackHandler {
        videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
        videoPlaybackViewModel.reportPlaybackStop(
            mediaId = currentMediaItem.id,
            userId = userId,
            accessToken = accessToken,
            positionSeconds = playbackProgress,
            isCompleted = playbackProgress >= effectiveDuration - 5
        )
        onBackClick()
    }

    DisposableEffect(Unit) {
        videoPlaybackViewModel.reportPlaybackStart(currentMediaItem.id, userId, accessToken)
        onDispose {
            videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
            videoPlaybackViewModel.reportPlaybackStop(
                mediaId = currentMediaItem.id,
                userId = userId,
                accessToken = accessToken,
                positionSeconds = playbackProgress,
                isCompleted = playbackProgress >= effectiveDuration - 5
            )
        }
    }

    LaunchedEffect(displayMode, isMpvInitialized) {
        if (isMpvInitialized) {
            handleDisplayModeChange(displayMode)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val mpvView = LocalView.current
        DisposableEffect(mpvView) {
            mpvView.keepScreenOn = true
            onDispose { mpvView.keepScreenOn = false }
        }
        MPVPlayer(
            modifier = Modifier
                .fillMaxSize()
                .let {
                    if (gesturesEnabled) it.pointerInput(Unit) {
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
                                startOffset = offset
                                startProgress = playbackProgress.toFloat()
                                startVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
                                startBrightness = brightness
                                isHorizontal = null
                            },
                            onDrag = { change, _ ->
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
                                    val newPos = (startProgress + delta).coerceIn(0f, effectiveDuration.toFloat())
                                    playbackProgress = newPos.roundToLong()
                                    MPVLib.setPropertyDouble("time-pos", newPos.toDouble())
                                    gestureFeedback = GestureFeedback.Seek(newPos.roundToLong())
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
                                        audioManager.setStreamVolume(
                                            AudioManager.STREAM_MUSIC,
                                            newV.roundToInt(),
                                            0
                                        )
                                        val percent = newV / maxVolume * 100f
                                        volume = percent.roundToLong()
                                        MPVLib.setPropertyInt("volume", percent.roundToInt())
                                        videoPlaybackViewModel.updateVolume(volume)
                                        gestureFeedback = GestureFeedback.Volume(volume.toInt())
                                    }
                                }
                                change.consume()
                            },
                            onDragEnd = {
                                if (isHorizontal == true) {
                                    videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
                                    isSeeking = false
                                }
                                gestureFeedback = null
                            },
                            onDragCancel = {
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
                    if (gesturesEnabled) it.pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                val half = size.width / 2
                                if (offset.x < half) {
                                    MPVLib.command(arrayOf("seek", "-10"))
                                    gestureFeedback = GestureFeedback.Text("-10")
                                } else {
                                    MPVLib.command(arrayOf("seek", "10"))
                                    gestureFeedback = GestureFeedback.Text("+10")
                                }
                            },
                            onTap = {
                                showControls = !showControls
                            }
                        )
                    } else it
                },
            onInitialized = {
                MPVLib.addLogObserver(DefaultLogObserver())
                MPVLib.setOptionString("android-page-size", RuntimeConfig.pageSize.toString())
                isMpvInitialized = true
                handleDisplayModeChange(displayMode)

                val resolvedConfigFile = mpvConfigFile
                    ?: ensureMpvConfigFile(context).also { mpvConfigFile = it }

                when (decoderMode) {
                    DecoderMode.HARDWARE_ONLY -> {
                        MPVLib.setOptionString("hwdec", "mediacodec-copy")
                        MPVLib.setOptionString("vo", "gpu-next")
                        MPVLib.setOptionString("vd-lavc-software-fallback", "no")
                        MPVLib.setOptionString("cache", "yes")
                        MPVLib.setOptionString("demuxer-max-bytes", (100 * 1024 * 1024).toString())
                        applyManualMpvDefaults()
                    }
                    DecoderMode.SOFTWARE_ONLY -> {
                        MPVLib.setOptionString("hwdec", "no")
                        MPVLib.setOptionString("vd", "lavc")
                        MPVLib.setOptionString("vo", "gpu-next")
                        MPVLib.setOptionString("gpu-api", "opengl")
                        MPVLib.setOptionString("cache", "yes")
                        MPVLib.setOptionString("demuxer-max-bytes", (100 * 1024 * 1024).toString())
                        applyManualMpvDefaults()
                    }
                    else -> {
                        applyMpvConfigFromFile(resolvedConfigFile)
                    }
                }

                currentAudioTrack?.let { audioIndex ->
                    audioIndexToMpvTrackIds[audioIndex]?.let { mpvAudioId ->
                        MPVLib.setOptionString("aid", mpvAudioId.toString())
                    }
                }

                val initialSubtitleStream = subtitleStreams.firstOrNull { it.index == currentSubtitleTrack }
                val initialMpvSubtitleIndex = currentSubtitleTrack?.let { subtitleIndexToMpvTrackIds[it] }
                if (
                    initialSubtitleStream?.isExternal == true ||
                    initialMpvSubtitleIndex == null ||
                    initialMpvSubtitleIndex < 0
                ) {
                    MPVLib.setOptionString("sid", "no")
                    MPVLib.setOptionString("sub-visibility", "no")
                } else {
                    MPVLib.setOptionString("sid", initialMpvSubtitleIndex.toString())
                    MPVLib.setOptionString("sub-visibility", "yes")
                }

                configureSubtitleFonts(
                    subtitleStreams.firstOrNull()?.codec,
                    context,
                    getSubtitleFont(initialSubtitleLanguage)
                )

                MPVLib.setPropertyBoolean("keep-open", true)
                MPVLib.setPropertyInt("volume", volume.toInt())
            },
            observedProperties = {
                long("duration")
                long("time-pos")
                boolean("eof-reached")
                boolean("pause")
                boolean("paused-for-cache")
                int("volume")
                double("speed")
                boolean("seeking")
                boolean("sub-visibility")
            },
            propertyObserver = {
                long("duration") { duration ->
                    val overrideDuration = playerState.totalDurationSeconds
                    playbackDuration = if (overrideDuration != null && overrideDuration > 0) {
                        overrideDuration
                    } else {
                        duration
                    }
                }
                long("time-pos") { timePos ->
                    if (!isSeeking) {
                        playbackProgress = timePos
                        videoPlaybackViewModel.updatePlaybackPosition(timePos * 1000)
                    }
                }
                boolean("eof-reached") { eofReached ->
                    if (eofReached) {
                        scope.launch {
                            videoPlaybackViewModel.reportPlaybackStop(
                                mediaId = latestMediaItem.id,
                                userId = userId,
                                accessToken = accessToken,
                                positionSeconds = effectiveDuration,
                                isCompleted = true
                            )
                            videoPlaybackViewModel.markAsWatched(latestMediaItem.id, userId)
                            if (hasNextEpisode && autoPlayNext) {
                                videoPlaybackViewModel.playNextEpisode()
                            } else if (!hasNextEpisode && latestMediaItem.type == ApiConstants.ITEM_TYPE_EPISODE) {
                                onBackClick()
                            }
                        }
                    }
                }
                boolean("pause") { paused ->
                    isPlaying = !paused
                    videoPlaybackViewModel.updatePausedState(paused)
                }
                boolean("paused-for-cache") { pausedForCache ->
                    isBuffering = pausedForCache
                    videoPlaybackViewModel.updateBufferingState(pausedForCache)
                }
                long("volume") { vol ->
                    volume = vol
                    videoPlaybackViewModel.updateVolume(vol)
                }
                double("speed") { spd ->
                    speed = spd.toFloat()
                    videoPlaybackViewModel.updatePlaybackSpeed(spd.toFloat())
                }
                boolean("seeking") { seeking ->
                    isSeeking = seeking
                }
                boolean("sub-visibility") { visible ->
                    Log.d("SubtitleVisibility", "Subtitle visibility: $visible")
                }
            }
        )

        LaunchedEffect(playerState.startPositionMs, playerState.startPositionUpdateCount) {
            MPVLib.command(
                arrayOf(
                    "seek",
                    (playerState.startPositionMs / 1000).toString(),
                    "absolute",
                    "exact"
                )
            )
            playbackProgress = playerState.startPositionMs / 1000
        }

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
                    if (isPlaying) {
                        videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
                    }
                    isPlaying = !isPlaying
                    MPVLib.setPropertyBoolean("pause", !isPlaying)
                },
                onSeek = { position ->
                    isSeeking = true
                    playbackProgress = position
                },
                onSeekComplete = { position ->
                    MPVLib.command(arrayOf("seek", position.toString(), "absolute"))
                    isSeeking = false
                    videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, position)
                },
                onBackClick = {
                    videoPlaybackViewModel.savePlaybackPosition(currentMediaItem.id, userId, accessToken, playbackProgress)
                    videoPlaybackViewModel.reportPlaybackStop(
                        mediaId = currentMediaItem.id,
                        userId = userId,
                        accessToken = accessToken,
                        positionSeconds = playbackProgress,
                        isCompleted = playbackProgress >= effectiveDuration - 5
                    )
                    onBackClick()
                },
                onSkipBackward = {
                    MPVLib.command(arrayOf("seek", "-10"))
                },
                onSkipForward = {
                    MPVLib.command(arrayOf("seek", "10"))
                },
                skipButtonVisible = showOverlaySkipButton,
                skipButtonLabel = skipLabel,
                onSkipButtonClick = {
                    videoPlaybackViewModel.skipSegment()
                    currentSkipSegmentId = null
                    skipSegmentEndMs = 0L
                    skipPromptDeadlineMs = 0L
                    showOverlaySkipButton = false
                    showFloatingSkipButton = false
                    showControls = false
                },
                onPlayPrevious = if (playerState.hasPreviousEpisode) {
                    {
                        videoPlaybackViewModel.savePlaybackPosition(
                            currentMediaItem.id,
                            userId,
                            accessToken,
                            playbackProgress
                        )
                        videoPlaybackViewModel.reportPlaybackStop(
                            mediaId = currentMediaItem.id,
                            userId = userId,
                            accessToken = accessToken,
                            positionSeconds = playbackProgress,
                            isCompleted = playbackProgress >= effectiveDuration - 5
                        )
                        videoPlaybackViewModel.playPreviousEpisode()
                    }
                } else {
                    null
                },
                onPlayNext = if (playerState.hasNextEpisode) {
                    {
                        videoPlaybackViewModel.savePlaybackPosition(
                            currentMediaItem.id,
                            userId,
                            accessToken,
                            playbackProgress
                        )
                        videoPlaybackViewModel.reportPlaybackStop(
                            mediaId = currentMediaItem.id,
                            userId = userId,
                            accessToken = accessToken,
                            positionSeconds = playbackProgress,
                            isCompleted = playbackProgress >= effectiveDuration - 5
                        )
                        videoPlaybackViewModel.playNextEpisode()
                    }
                } else {
                    null
                },
                speed = speed,
                onSpeedChange = { newSpeed ->
                    speed = newSpeed
                    MPVLib.setPropertyDouble("speed", newSpeed.toDouble())
                },
                playbackQualityLabel = playerState.currentTranscodeQualityText,
                onPlaybackQualityClick = if (canTranscode) {
                    { showTranscodeDialog = true }
                } else {
                    null
                },
                onAudioClick = { showAudioDialog = true },
                onSubtitleClick = { showSubtitleDialog = true },
                onDecoderClick = { showDecoderDialog = true },
                onDisplayClick = { showDisplayDialog = true },
                onPipClick = {
                    val width = MPVLib.getPropertyInt("dwidth")?.coerceAtLeast(1)
                    val height = MPVLib.getPropertyInt("dheight")?.coerceAtLeast(1)
                    val params = PictureInPictureParams.Builder()
                        .setAspectRatio(Rational(width!!, height!!))
                        .build()
                    activity?.enterPictureInPictureMode(params)
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
                        videoPlaybackViewModel.skipSegment()
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
                    videoPlaybackViewModel.selectPlaybackTranscodeOption(option)
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
                    handleAudioTrackChange(stream.index)
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
                    handleSubtitleTrackChange(stream?.index)
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
                    handleDisplayModeChange(mode)
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
                    val pos = playbackProgress
                    when (mode) {
                        DecoderMode.HARDWARE_ONLY -> {
                            MPVLib.setOptionString("hwdec", "mediacodec")
                            MPVLib.setOptionString("vd", "auto")
                            MPVLib.setOptionString("vd-lavc-software-fallback", "no")
                        }
                        DecoderMode.SOFTWARE_ONLY -> {
                            MPVLib.setOptionString("hwdec", "no")
                            MPVLib.setOptionString("gpu-api", "opengl")
                            MPVLib.setOptionString("vd", "lavc")
                            MPVLib.setOptionString("vd-lavc-software-fallback", "yes")
                        }
                        else -> {
                            MPVLib.setOptionString("hwdec", "no")
                            MPVLib.setOptionString("vd", "auto")
                        }
                    }
                    resolvedVideoUrl?.let { url ->
                        MPVLib.command(arrayOf("loadfile", url, "replace"))
                        MPVLib.command(arrayOf("seek", pos.toString(), "absolute", "exact"))
                        MPVLib.setPropertyBoolean("pause", !isPlaying)
                    }
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

        GestureFeedbackOverlay(
            feedback = gestureFeedback,
            modifier = Modifier.align(Alignment.Center)
        )
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

private suspend fun ensureExternalSubtitleFile(
    context: Context,
    mediaItem: MediaItem,
    stream: MediaStream,
    serverUrl: String,
    accessToken: String,
    client: OkHttpClient,
    cache: MutableMap<Int, File>
): File? {
    val extension = stream.codec.toSubtitleFileExtension()
    getLocalSubtitleFile(context, mediaItem.id, stream.index, extension)?.let { return it }

    cache[stream.index]?.takeIf { it.exists() }?.let { return it }

    val subtitleUrl = mediaItem.getSubtitleUrl(serverUrl, accessToken, stream.index)
    val downloaded = downloadExternalSubtitle(
        context = context,
        mediaId = mediaItem.id,
        index = stream.index,
        extension = extension,
        url = subtitleUrl,
        client = client
    )

    if (downloaded != null) {
        cache[stream.index] = downloaded
    }

    return downloaded
}

private fun ensureMpvConfigFile(context: Context): File {
    val directory = File(context.filesDir, MpvConstants.CONFIG_DIRECTORY)
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val file = File(directory, MpvConstants.CONFIG_FILE_NAME)
    if (!file.exists() || file.length() == 0L) {
        file.writeText(MpvConstants.DEFAULT_CONFIG)
    }
    return file
}

private fun applyMpvConfigFromFile(configFile: File) {
    if (!configFile.exists()) {
        return
    }
    runCatching {
        configFile.useLines { lines ->
            lines.forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    return@forEach
                }
                val parts = line.split('=', limit = 2)
                val key = parts.getOrNull(0)?.trim().orEmpty()
                if (key.isEmpty()) {
                    return@forEach
                }
                val value = parts.getOrNull(1)?.trim().orEmpty()
                MPVLib.setOptionString(key, value)
            }
        }
    }.onFailure { error ->
        Log.e("MpvPlayerView", "Failed to apply mpv config", error)
    }
}

private fun applyManualMpvDefaults() {
    MPVLib.setOptionString("vd-lavc-assume-old-x264", "yes")
    MPVLib.setOptionString("codec-profile", "custom")
    MPVLib.setOptionString("vd-lavc-check-hw-profile", "no")
    MPVLib.setOptionString("vd-lavc-codec-whitelist", "h264,hevc,vp8,vp9,av1,mpeg2video,mpeg4")
    MPVLib.setOptionString("sub-codepage", "auto")
    MPVLib.setOptionString("sub-fix-timing", "yes")
    MPVLib.setOptionString("blend-subtitles", "yes")
    MPVLib.setOptionString("sub-forced-only", "no")
}

private suspend fun downloadExternalSubtitle(
    context: Context,
    mediaId: String,
    index: Int,
    extension: String,
    url: String,
    client: OkHttpClient
): File? = withContext(Dispatchers.IO) {
    try {
        val request = Request.Builder().url(url).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(
                    "MpvPlayerView",
                    "Failed to download external subtitle $index (HTTP ${'$'}{response.code})"
                )
                return@withContext null
            }

            val body = response.body ?: return@withContext null
            val directory = File(context.cacheDir, "mpv/subtitles").apply { mkdirs() }
            val file = File(directory, "$mediaId-$index.$extension")
            body.byteStream().use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file
        }
    } catch (io: IOException) {
        Log.e("MpvPlayerView", "Error downloading external subtitle $index", io)
        null
    }
}

internal fun configureSubtitleFonts(
    codec: String?,
    context: Context,
    fallbackFont: String
) {
    MPVLib.setOptionString("embeddedfonts", "yes")
    MPVLib.setOptionString("sub-fonts-dir", File(context.filesDir, "fonts").absolutePath)
    MPVLib.setOptionString("sub-font-provider", "file")
    MPVLib.setOptionString("sub-font", fallbackFont)

    if (codec?.lowercase() !in listOf("ass", "ssa")) {
        MPVLib.setOptionString("sub-ass", "yes")
        MPVLib.setOptionString("sub-ass-override", "no")
        MPVLib.setOptionString("sub-font-size", "48")
        MPVLib.setOptionString("sub-color", "#FFFFFFFF")
        MPVLib.setOptionString("sub-shadow-color", "#80000000")
        MPVLib.setOptionString("sub-shadow-offset", "1")
        MPVLib.setOptionString("sub-margin-y", "20")
        MPVLib.setOptionString("sub-align-y", "bottom")
    }
}