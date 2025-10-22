package com.hritwik.avoid.presentation.viewmodel.player

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.core.ServiceManager
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.dao.MediaItemDao
import com.hritwik.avoid.data.local.database.dao.PlaybackLogDao
import com.hritwik.avoid.data.local.database.entities.DownloadEntity
import com.hritwik.avoid.data.local.database.entities.PlaybackLogEntity
import com.hritwik.avoid.data.local.model.PlaybackPreferences
import com.hritwik.avoid.data.sync.PlaybackLogSyncWorker
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.media.PlaybackOptions
import com.hritwik.avoid.domain.model.media.VideoQuality
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.library.GetMediaDetailUseCase
import com.hritwik.avoid.domain.model.playback.PlaybackTranscodeOption
import com.hritwik.avoid.domain.model.playback.PreferredAudioCodec
import com.hritwik.avoid.domain.model.playback.PreferredVideoCodec
import com.hritwik.avoid.presentation.ui.state.TrackChangeEvent
import com.hritwik.avoid.presentation.ui.state.VideoPlaybackState
import com.hritwik.avoid.utils.extensions.getStreamContainer
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import com.hritwik.avoid.utils.constants.ApiConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class VideoPlaybackViewModel @Inject constructor(
    private val mediaItemDao: MediaItemDao,
    private val preferencesManager: PreferencesManager,
    private val libraryRepository: LibraryRepository,
    private val downloadDao: DownloadDao,
    private val playbackLogDao: PlaybackLogDao,
    private val getMediaDetailUseCase: GetMediaDetailUseCase,
    private val serviceManager: ServiceManager,
    @ApplicationContext private val context: Context,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel(connectivityObserver) {
    private val _state = MutableStateFlow(VideoPlaybackState())
    val state: StateFlow<VideoPlaybackState> = _state.asStateFlow()
    private val _trackChangeEvents = MutableSharedFlow<TrackChangeEvent>()
    val trackChangeEvents: SharedFlow<TrackChangeEvent> = _trackChangeEvents.asSharedFlow()
    private var currentMediaSourceId: String? = null
    private var currentAudioStreamIndex: Int? = null
    private var currentSubtitleStreamIndex: Int? = null
    private var savedUserId: String? = null
    private var savedAccessToken: String? = null
    private var savedServerUrl: String? = null
    private var selectedTranscodeOption: PlaybackTranscodeOption = PlaybackTranscodeOption.ORIGINAL
    private var preferredVideoCodec: PreferredVideoCodec = PreferredVideoCodec.H264
    private var preferredAudioCodec: PreferredAudioCodec = PreferredAudioCodec.AAC

    init {
        viewModelScope.launch {
            preferencesManager.getPreferredVideoCodec().collect { codec ->
                val previous = preferredVideoCodec
                preferredVideoCodec = codec
                if (!selectedTranscodeOption.isOriginal && codec != previous) {
                    rebuildWithCurrentSelection()
                }
            }
        }
        viewModelScope.launch {
            preferencesManager.getPreferredAudioCodec().collect { codec ->
                val previous = preferredAudioCodec
                preferredAudioCodec = codec
                if (!selectedTranscodeOption.isOriginal && codec != previous) {
                    rebuildWithCurrentSelection()
                }
            }
        }
    }

    fun initializeVideoOptions(
        mediaItem: MediaItem,
        userId: String,
        accessToken: String
    ) {
        savedUserId = userId
        savedAccessToken = accessToken
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            if (!mediaItem.hasPlaybackDataLoaded()) {
                val download = downloadDao.getDownloadByMediaId(mediaItem.id)
                val isOffline = !isConnected.value
                if (download != null) {
                    val offlineItem = buildOfflineMediaItem(mediaItem, download)
                    setupVideoOptions(offlineItem)
                } else {
                    when (val result = getMediaDetailUseCase(
                        GetMediaDetailUseCase.Params(mediaItem.id, userId, accessToken)
                    )) {
                        is NetworkResult.Success -> {
                            setupVideoOptions(result.data)
                        }
                        is NetworkResult.Error -> {
                            val fallbackDownload = downloadDao.getDownloadByMediaId(mediaItem.id)
                            if (fallbackDownload != null) {
                                val offlineItem = buildOfflineMediaItem(mediaItem, fallbackDownload)
                                setupVideoOptions(offlineItem)
                            } else {
                                val errorMessage = if (isOffline) "No network connection" else result.message
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = errorMessage
                                )
                            }
                        }
                        is NetworkResult.Loading -> {
                            
                        }
                    }
                }
            } else {
                setupVideoOptions(mediaItem)
            }
        }
    }

    private fun buildOfflineMediaItem(mediaItem: MediaItem, download: DownloadEntity): MediaItem {
        val streams = buildList {
            download.defaultVideoStream?.let { add(it) }
            addAll(download.audioStreams)
            addAll(download.subtitleStreams)
        }

        val offlineSource = MediaSource(
            id = download.mediaSourceId ?: mediaItem.id,
            name = null,
            type = null,
            container = null,
            size = null,
            bitrate = null,
            path = null,
            protocol = null,
            runTimeTicks = mediaItem.runTimeTicks,
            videoType = null,
            mediaStreams = streams,
            isRemote = false,
            supportsTranscoding = false,
            supportsDirectStream = true,
            supportsDirectPlay = true
        )

        return mediaItem.copy(mediaSources = listOf(offlineSource))
    }

    private suspend fun setupVideoOptions(mediaItem: MediaItem) {
        val primarySource = mediaItem.getPrimaryMediaSource()

        val preferredAudioLanguage = preferencesManager.getAudioTrackLanguage().first()
        val preferredSubtitleLanguage = preferencesManager.getSubtitleLanguage().first()
        val savedPrefs = preferencesManager.getPlaybackPreferences(mediaItem.id).first()

        val selectedMediaSource = savedPrefs?.mediaSourceId?.let { id ->
            mediaItem.mediaSources.firstOrNull { it.id == id }
        } ?: primarySource

        val subtitleStreams = selectedMediaSource?.subtitleStreams ?: emptyList()

        val selectedVideoStream = savedPrefs?.videoQuality?.let { qualityStr ->
            runCatching { VideoQuality.valueOf(qualityStr) }.getOrNull()?.let { q ->
                selectedMediaSource?.videoStreams?.firstOrNull { it.videoQuality == q }
            }
        } ?: selectedMediaSource?.defaultVideoStream

        val selectedAudioStream = savedPrefs?.audioIndex?.let { idx ->
            selectedMediaSource?.audioStreams?.firstOrNull { it.index == idx }
        } ?: selectedMediaSource?.audioStreams?.firstOrNull {
            it.language?.equals(preferredAudioLanguage, true) == true
        } ?: selectedMediaSource?.defaultAudioStream

        val selectedSubtitleStream = savedPrefs?.subtitleIndex?.let { idx ->
            subtitleStreams.firstOrNull { it.index == idx }
        } ?: subtitleStreams.firstOrNull {
            it.language?.equals(preferredSubtitleLanguage, true) == true
        } ?: selectedMediaSource?.defaultSubtitleStream

        val initialPlaybackOptions = PlaybackOptions(
            selectedMediaSource = selectedMediaSource,
            selectedVideoStream = selectedVideoStream,
            selectedAudioStream = selectedAudioStream,
            selectedSubtitleStream = selectedSubtitleStream,
            resumePositionTicks = mediaItem.userData?.playbackPositionTicks ?: 0L
        )

        val downloadPath = serviceManager.getDownloadedFilePath(mediaItem.id)
        val transcodeOptions = if (downloadPath != null) {
            selectedTranscodeOption = PlaybackTranscodeOption.ORIGINAL
            listOf(PlaybackTranscodeOption.ORIGINAL)
        } else {
            PlaybackTranscodeOption.entries.toList()
        }

        _state.value = _state.value.copy(
            isLoading = false,
            mediaItem = mediaItem,
            playbackOptions = initialPlaybackOptions,
            availableVersions = mediaItem.mediaSources,
            availableVideoQualities = mediaItem.getAvailableVideoQualities(),
            availableAudioStreams = selectedMediaSource?.audioStreams ?: emptyList(),
            availableSubtitleStreams = subtitleStreams,
            preferredAudioLanguage = preferredAudioLanguage,
            preferredSubtitleLanguage = preferredSubtitleLanguage,
            playbackTranscodeOptions = transcodeOptions,
            selectedPlaybackTranscodeOption = selectedTranscodeOption,
            totalDurationSeconds = mediaItem.runTimeTicks?.div(10_000_000L)
        )

        if (downloadPath != null) {
            _state.value = _state.value.copy(
                videoUrl = Uri.fromFile(File(downloadPath)).toString(),
                exoMediaItem = null
            )
        }
    }

    fun showVersionDialog() { _state.value = _state.value.copy(showVersionDialog = true) }
    fun hideVersionDialog() { _state.value = _state.value.copy(showVersionDialog = false) }
    fun showVideoQualityDialog() { _state.value = _state.value.copy(showVideoQualityDialog = true) }
    fun hideVideoQualityDialog() { _state.value = _state.value.copy(showVideoQualityDialog = false) }
    fun showAudioDialog() { _state.value = _state.value.copy(showAudioDialog = true) }
    fun hideAudioDialog() { _state.value = _state.value.copy(showAudioDialog = false) }
    fun showSubtitleDialog() { _state.value = _state.value.copy(showSubtitleDialog = true) }
    fun hideSubtitleDialog() { _state.value = _state.value.copy(showSubtitleDialog = false) }

    private fun persistPlaybackPreferences() {
        val mediaId = _state.value.mediaItem?.id ?: return
        val options = _state.value.playbackOptions
        val prefs = PlaybackPreferences(
            mediaSourceId = options.selectedMediaSource?.id,
            audioIndex = options.selectedAudioStream?.index,
            subtitleIndex = options.selectedSubtitleStream?.index,
            videoQuality = options.selectedVideoStream?.videoQuality?.name
        )
        viewModelScope.launch {
            preferencesManager.savePlaybackPreferences(mediaId, prefs)
        }
    }

    fun selectVersion(mediaSource: MediaSource) {
        viewModelScope.launch {
            var updatedSource = mediaSource
            if (mediaSource.mediaStreams.isEmpty()) {
                val mediaId = _state.value.mediaItem?.id
                val userId = savedUserId
                val accessToken = savedAccessToken
                if (mediaId != null && userId != null && accessToken != null) {
                    when (val result = libraryRepository.getMediaItemDetail(userId, mediaId, accessToken)) {
                        is NetworkResult.Success -> {
                            val item = result.data
                            updatedSource = item.mediaSources.firstOrNull { it.id == mediaSource.id } ?: mediaSource
                            _state.value = _state.value.copy(
                                mediaItem = item,
                                availableVersions = item.mediaSources
                            )
                        }
                        is NetworkResult.Error -> {
                            _state.value = _state.value.copy(
                                error = result.message,
                                showVersionDialog = false
                            )
                            return@launch
                        }
                        else -> return@launch
                    }
                }
            }

            currentMediaSourceId = updatedSource.id
            currentAudioStreamIndex = updatedSource.defaultAudioStream?.index
            currentSubtitleStreamIndex = updatedSource.defaultSubtitleStream?.index
            val availableVideoQualities = updatedSource.availableVideoQualities
            val updatedOptions = _state.value.playbackOptions.copy(
                selectedMediaSource = updatedSource,
                selectedVideoStream = updatedSource.defaultVideoStream,
                selectedAudioStream = updatedSource.defaultAudioStream,
                selectedSubtitleStream = updatedSource.defaultSubtitleStream
            )

            _state.value = _state.value.copy(
                playbackOptions = updatedOptions,
                showVersionDialog = false,
                mediaSourceId = currentMediaSourceId,
                audioStreamIndex = currentAudioStreamIndex,
                subtitleStreamIndex = currentSubtitleStreamIndex,
                availableVideoQualities = availableVideoQualities,
                availableAudioStreams = updatedSource.audioStreams,
                availableSubtitleStreams = updatedSource.subtitleStreams,
                preferredAudioLanguage = updatedOptions.selectedAudioStream?.language,
                preferredSubtitleLanguage = updatedOptions.selectedSubtitleStream?.language,
                totalDurationSeconds = updatedSource.runTimeTicks?.div(10_000_000L)
                    ?: _state.value.totalDurationSeconds
            )
            persistPlaybackPreferences()
        }
    }

    fun selectVideoQuality(quality: VideoQuality) {
        val currentSource = _state.value.playbackOptions.selectedMediaSource ?: return
        val videoStream = currentSource.videoStreams.firstOrNull { it.videoQuality == quality }

        if (videoStream != null) {
            val updatedOptions = _state.value.playbackOptions.copy(
                selectedVideoStream = videoStream
            )

            _state.value = _state.value.copy(
                playbackOptions = updatedOptions,
                showVideoQualityDialog = false
            )
        }
        persistPlaybackPreferences()
    }

    fun selectAudioStream(audioStream: MediaStream) {
        val updatedOptions = _state.value.playbackOptions.copy(
            selectedAudioStream = audioStream
        )

        _state.value = _state.value.copy(
            playbackOptions = updatedOptions,
            showAudioDialog = false,
            preferredAudioLanguage = audioStream.language
        )

        viewModelScope.launch {
            preferencesManager.saveAudioTrackLanguage(audioStream.language)
        }
        persistPlaybackPreferences()
    }

    fun selectSubtitleStream(subtitleStream: MediaStream?) {
        val updatedOptions = _state.value.playbackOptions.copy(
            selectedSubtitleStream = subtitleStream
        )

        _state.value = _state.value.copy(
            playbackOptions = updatedOptions,
            showSubtitleDialog = false,
            preferredSubtitleLanguage = subtitleStream?.language
        )

        viewModelScope.launch {
            preferencesManager.saveSubtitleLanguage(subtitleStream?.language)
        }
        persistPlaybackPreferences()
    }

    fun selectResumePlayback(resumePositionTicks: Long) {
        val updatedOptions = _state.value.playbackOptions.copy(
            startFromBeginning = false,
            resumePositionTicks = resumePositionTicks
        )
        _state.value = _state.value.copy(playbackOptions = updatedOptions)
    }

    fun selectStartFromBeginning() {
        val updatedOptions = _state.value.playbackOptions.copy(
            startFromBeginning = true,
            resumePositionTicks = 0L
        )
        _state.value = _state.value.copy(playbackOptions = updatedOptions)
    }

    
    
    

    fun initializePlayer(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        userId: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        startPositionMs: Long = 0
    ) {
        savedServerUrl = serverUrl
        savedUserId = userId
        savedAccessToken = accessToken
        _state.value = _state.value.copy(mediaItem = mediaItem)
        viewModelScope.launch {
            try {
                val isOffline = !isConnected.value
                var resolvedStartMs = startPositionMs
                if (resolvedStartMs == 0L) {
                    val localTicks = preferencesManager.getPlaybackPosition(mediaItem.id).first()
                    resolvedStartMs = localTicks?.div(10_000) ?: 0L
                    if (resolvedStartMs == 0L && !isOffline) {
                        resolvedStartMs = when (val result = libraryRepository.getPlaybackPosition(userId, mediaItem.id, accessToken)) {
                            is NetworkResult.Success -> result.data / 10_000
                            else -> 0L
                        }
                    }
                    if (resolvedStartMs == 0L) {
                        _state.value = _state.value.copy(error = "Unable to restore playback progress")
                    }
                }
                if (resolvedStartMs == 0L) {
                    _state.value = _state.value.copy(error = "Unable to restore playback progress")
                }
                val savedPrefs = if (
                    mediaSourceId == null || audioStreamIndex == null || subtitleStreamIndex == null
                ) {
                    preferencesManager.getPlaybackPreferences(mediaItem.id).first()
                } else null

                currentMediaSourceId = mediaSourceId
                    ?: currentMediaSourceId
                            ?: savedPrefs?.mediaSourceId
                            ?: mediaItem.mediaSources.firstOrNull()?.id
                            ?: mediaItem.id
                currentAudioStreamIndex = audioStreamIndex
                    ?: currentAudioStreamIndex
                            ?: savedPrefs?.audioIndex
                            ?: mediaItem.mediaSources.firstOrNull { it.id == currentMediaSourceId }?.defaultAudioStream?.index
                currentSubtitleStreamIndex = subtitleStreamIndex
                    ?: currentSubtitleStreamIndex
                            ?: savedPrefs?.subtitleIndex
                            ?: mediaItem.mediaSources.firstOrNull { it.id == currentMediaSourceId }?.defaultSubtitleStream?.index

                val selectedSource = mediaItem.mediaSources.firstOrNull { it.id == currentMediaSourceId }
                var availableAudioStreams = selectedSource?.audioStreams ?: emptyList()
                var availableSubtitleStreams = selectedSource?.subtitleStreams ?: emptyList()

                if (isOffline && (selectedSource == null || (availableAudioStreams.isEmpty() && availableSubtitleStreams.isEmpty()))) {
                    val download = downloadDao.getDownloadByMediaId(mediaItem.id)
                    if (download != null) {
                        availableAudioStreams = download.audioStreams
                        availableSubtitleStreams = download.subtitleStreams
                    }
                }

                val selectedAudioStream = currentAudioStreamIndex?.let { idx ->
                    availableAudioStreams.firstOrNull { it.index == idx }
                }
                val selectedSubtitleStream = currentSubtitleStreamIndex?.let { idx ->
                    availableSubtitleStreams.firstOrNull { it.index == idx }
                }


                if (!isOffline) {
                    when (val segmentResult = libraryRepository.getItemSegments(
                        userId = userId,
                        mediaId = mediaItem.id,
                        accessToken = accessToken
                    )) {
                        is NetworkResult.Success -> {
                            _state.value = _state.value.copy(segments = segmentResult.data)
                            updatePlaybackPosition(resolvedStartMs)
                        }
                        else -> {
                            val errorMessage = if (segmentResult is NetworkResult.Error) {
                                segmentResult.message
                            } else {
                                "Unknown error"
                            }
                            Log.e(
                                "VideoPlaybackViewModel",
                                "Failed to load item segments: $errorMessage",
                                (segmentResult as? NetworkResult.Error)?.exception
                            )
                            _state.value = _state.value.copy(
                                segments = emptyList(),
                                activeSegment = null,
                                error = errorMessage
                            )
                        }
                    }
                }


                val filePath = serviceManager.getDownloadedFilePath(mediaItem.id)
                if (filePath != null) {
                    selectedTranscodeOption = PlaybackTranscodeOption.ORIGINAL
                    val nextUpdateCount = _state.value.startPositionUpdateCount + 1
                    _state.value = _state.value.copy(
                        videoUrl = Uri.fromFile(File(filePath)).toString(),
                        exoMediaItem = null,
                        cacheDataSourceFactory = null,
                        mediaSourceId = currentMediaSourceId,
                        audioStreamIndex = currentAudioStreamIndex,
                        subtitleStreamIndex = currentSubtitleStreamIndex,
                        startPositionMs = resolvedStartMs,
                        playbackOffsetMs = 0,
                        startPositionUpdateCount = nextUpdateCount,
                        isInitialized = true,
                        availableAudioStreams = availableAudioStreams,
                        availableSubtitleStreams = availableSubtitleStreams,
                        playbackTranscodeOptions = listOf(PlaybackTranscodeOption.ORIGINAL),
                        selectedPlaybackTranscodeOption = selectedTranscodeOption,
                        totalDurationSeconds = mediaItem.runTimeTicks?.div(10_000_000L),
                        lastSavedPosition = resolvedStartMs / 1000,
                        playbackOptions = _state.value.playbackOptions.copy(
                            selectedMediaSource = selectedSource,
                            selectedAudioStream = selectedAudioStream,
                            selectedSubtitleStream = selectedSubtitleStream
                        )
                    )
                    if (mediaItem.type.equals(ApiConstants.ITEM_TYPE_EPISODE, true)) {
                        updateEpisodeNavigation(mediaItem, userId, accessToken)
                    } else {
                        _state.value = _state.value.copy(
                            siblingEpisodes = emptyList(),
                            currentEpisodeIndex = -1
                        )
                    }
                    if (currentAudioStreamIndex != null || currentSubtitleStreamIndex != null) {
                        viewModelScope.launch {
                            _trackChangeEvents.emit(
                                TrackChangeEvent(currentAudioStreamIndex, currentSubtitleStreamIndex)
                            )
                        }
                    }
                    return@launch
                }

                val container = mediaItem.getStreamContainer(currentMediaSourceId) ?: "mkv"

                val startOffsetMs = if (selectedTranscodeOption.isOriginal) 0L else resolvedStartMs
                val playerStartMs = if (selectedTranscodeOption.isOriginal) resolvedStartMs else 0L
                val videoUrl = buildVideoUrl(
                    serverUrl = serverUrl,
                    accessToken = accessToken,
                    mediaItem = mediaItem,
                    mediaSourceId = currentMediaSourceId
                        ?: mediaItem.mediaSources.firstOrNull()?.id
                        ?: mediaItem.id,
                    container = container,
                    audioStreamIndex = currentAudioStreamIndex,
                    subtitleStreamIndex = currentSubtitleStreamIndex,
                    transcodeOption = selectedTranscodeOption,
                    startPositionMs = if (selectedTranscodeOption.isOriginal) null else resolvedStartMs
                )

                val nextUpdateCount = _state.value.startPositionUpdateCount + 1
                val availableTranscodeOptions = PlaybackTranscodeOption.entries.toList()

                _state.value = _state.value.copy(
                    videoUrl = videoUrl,
                    exoMediaItem = null,
                    cacheDataSourceFactory = null,
                    mediaSourceId = currentMediaSourceId,
                    audioStreamIndex = currentAudioStreamIndex,
                    subtitleStreamIndex = currentSubtitleStreamIndex,
                    startPositionMs = playerStartMs,
                    playbackOffsetMs = startOffsetMs,
                    startPositionUpdateCount = nextUpdateCount,
                    isInitialized = true,
                    availableAudioStreams = availableAudioStreams,
                    availableSubtitleStreams = availableSubtitleStreams,
                    playbackTranscodeOptions = availableTranscodeOptions,
                    selectedPlaybackTranscodeOption = selectedTranscodeOption,
                    lastSavedPosition = (startOffsetMs + playerStartMs) / 1000,
                    playbackOptions = _state.value.playbackOptions.copy(
                        selectedMediaSource = selectedSource,
                        selectedAudioStream = selectedAudioStream,
                        selectedSubtitleStream = selectedSubtitleStream
                    )
                )
                if (mediaItem.type.equals(ApiConstants.ITEM_TYPE_EPISODE, true)) {
                    updateEpisodeNavigation(mediaItem, userId, accessToken)
                } else {
                    _state.value = _state.value.copy(
                        siblingEpisodes = emptyList(),
                        currentEpisodeIndex = -1
                    )
                }
                if (currentAudioStreamIndex != null || currentSubtitleStreamIndex != null) {
                    viewModelScope.launch {
                        _trackChangeEvents.emit(
                            TrackChangeEvent(currentAudioStreamIndex, currentSubtitleStreamIndex)
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    error = "Failed to initialize player: ${e.message}"
                )
            }
        }
    }

    fun playNextEpisode() {
        val nextIndex = _state.value.currentEpisodeIndex + 1
        val episodes = _state.value.siblingEpisodes
        if (nextIndex in episodes.indices) {
            viewModelScope.launch {
                playEpisodeAtIndex(episodes[nextIndex], nextIndex)
            }
        }
    }

    fun playPreviousEpisode() {
        val previousIndex = _state.value.currentEpisodeIndex - 1
        val episodes = _state.value.siblingEpisodes
        if (previousIndex in episodes.indices) {
            viewModelScope.launch {
                playEpisodeAtIndex(episodes[previousIndex], previousIndex)
            }
        }
    }

    private suspend fun playEpisodeAtIndex(targetEpisode: MediaItem, targetIndex: Int) {
        val userId = savedUserId ?: return
        val accessToken = savedAccessToken ?: return
        val serverUrl = savedServerUrl ?: return
        val currentSourcePath = _state.value.playbackOptions.selectedMediaSource?.path

        val detailedEpisode = when (
            val result = getMediaDetailUseCase(
                GetMediaDetailUseCase.Params(targetEpisode.id, userId, accessToken)
            )
        ) {
            is NetworkResult.Success -> result.data
            is NetworkResult.Error -> targetEpisode
            is NetworkResult.Loading -> targetEpisode
        }

        setupVideoOptions(detailedEpisode)

        val preferredSource = selectPreferredMediaSource(currentSourcePath, detailedEpisode)
            ?: detailedEpisode.getPrimaryMediaSource()
        val preferredAudio = preferredSource?.defaultAudioStream
        val preferredSubtitle = preferredSource?.defaultSubtitleStream

        val updatedOptions = _state.value.playbackOptions.copy(
            selectedMediaSource = preferredSource ?: _state.value.playbackOptions.selectedMediaSource,
            selectedAudioStream = preferredAudio ?: _state.value.playbackOptions.selectedAudioStream,
            selectedSubtitleStream = preferredSubtitle ?: _state.value.playbackOptions.selectedSubtitleStream,
            startFromBeginning = true,
            resumePositionTicks = 0L
        )

        val updatedEpisodes = _state.value.siblingEpisodes.mapIndexed { index, item ->
            if (index == targetIndex) detailedEpisode else item
        }

        _state.value = _state.value.copy(
            mediaItem = detailedEpisode,
            playbackOptions = updatedOptions,
            availableAudioStreams = preferredSource?.audioStreams
                ?: updatedOptions.selectedMediaSource?.audioStreams.orEmpty(),
            availableSubtitleStreams = preferredSource?.subtitleStreams
                ?: updatedOptions.selectedMediaSource?.subtitleStreams.orEmpty(),
            currentEpisodeIndex = targetIndex,
            siblingEpisodes = updatedEpisodes,
            isInitialized = false,
            error = null
        )

        persistPlaybackPreferences()

        initializePlayer(
            mediaItem = detailedEpisode,
            serverUrl = serverUrl,
            accessToken = accessToken,
            userId = userId,
            mediaSourceId = preferredSource?.id,
            audioStreamIndex = preferredAudio?.index,
            subtitleStreamIndex = preferredSubtitle?.index,
            startPositionMs = 0
        )
    }

    private fun selectPreferredMediaSource(
        currentSourcePath: String?,
        episode: MediaItem
    ): MediaSource? {
        val currentParent = currentSourcePath?.let { extractParentPath(it) } ?: return null
        return episode.mediaSources.firstOrNull { source ->
            val candidateParent = source.path?.let { extractParentPath(it) }
            candidateParent != null && candidateParent.equals(currentParent, ignoreCase = true)
        }
    }

    private fun extractParentPath(path: String): String? {
        val normalized = path.trim().replace('\\', '/').trimEnd('/')
        val lastSeparator = normalized.lastIndexOf('/')
        return if (lastSeparator > 0) normalized.substring(0, lastSeparator) else null
    }

    private suspend fun updateEpisodeNavigation(
        mediaItem: MediaItem,
        userId: String,
        accessToken: String
    ) {
        if (!mediaItem.type.equals(ApiConstants.ITEM_TYPE_EPISODE, true)) {
            _state.value = _state.value.copy(siblingEpisodes = emptyList(), currentEpisodeIndex = -1)
            return
        }

        val seriesId = mediaItem.seriesId ?: run {
            _state.value = _state.value.copy(siblingEpisodes = emptyList(), currentEpisodeIndex = -1)
            return
        }

        val seasonsResult = libraryRepository.getSeasons(userId, seriesId, accessToken)
        val seasonId = when (seasonsResult) {
            is NetworkResult.Success -> {
                val seasons = seasonsResult.data
                val preferredSeason = mediaItem.parentIndexNumber?.let { index ->
                    seasons.firstOrNull { it.indexNumber == index }
                } ?: seasons.firstOrNull()
                preferredSeason?.id
            }
            else -> null
        }

        if (seasonId == null) {
            _state.value = _state.value.copy(siblingEpisodes = emptyList(), currentEpisodeIndex = -1)
            return
        }

        when (val episodesResult = libraryRepository.getEpisodes(userId, seasonId, accessToken)) {
            is NetworkResult.Success -> {
                val episodes = episodesResult.data
                val resolvedEpisodes = episodes.map { episode ->
                    if (episode.id == mediaItem.id) mediaItem else episode
                }
                val index = resolvedEpisodes.indexOfFirst { it.id == mediaItem.id }
                _state.value = _state.value.copy(
                    siblingEpisodes = resolvedEpisodes,
                    currentEpisodeIndex = index
                )
            }
            else -> {
                val existingIndex = _state.value.siblingEpisodes.indexOfFirst { it.id == mediaItem.id }
                if (existingIndex != -1) {
                    _state.value = _state.value.copy(currentEpisodeIndex = existingIndex)
                } else {
                    _state.value = _state.value.copy(
                        siblingEpisodes = emptyList(),
                        currentEpisodeIndex = -1
                    )
                }
            }
        }
    }

    private fun buildVideoUrl(
        serverUrl: String,
        accessToken: String,
        mediaItem: MediaItem,
        mediaSourceId: String,
        container: String = "mkv",
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null,
        transcodeOption: PlaybackTranscodeOption = PlaybackTranscodeOption.ORIGINAL,
        startPositionMs: Long? = null,
    ): String {
        return buildString {
            append(serverUrl.removeSuffix("/"))
            append("/Videos/")
            append(mediaItem.id)
            append("/stream")
            append("?static=")
            append(transcodeOption.isStaticStream)
            append("&container=")
            append(container)
            append("&MediaSourceId=")
            append(mediaSourceId)
            audioStreamIndex?.let { append("&AudioStreamIndex=$it") }
            subtitleStreamIndex?.let { append("&SubtitleStreamIndex=$it") }
            append("&api_key=")
            append(accessToken)
            if (!transcodeOption.isOriginal) {
                startPositionMs?.let { position ->
                    append("&StartTimeTicks=")
                    append(position * 10_000)
                }
            }
            val videoCodecOverride = if (transcodeOption.isOriginal) {
                null
            } else {
                preferredVideoCodec.codecQueryValue
            }
            val videoProfileOverride = if (transcodeOption.isOriginal) {
                null
            } else {
                preferredVideoCodec.profile
            }
            val (audioCodecOverride, allowAudioStreamCopyOverride) = when {
                transcodeOption.isOriginal -> null to null
                preferredAudioCodec == PreferredAudioCodec.ORIGINAL -> null to true
                else -> preferredAudioCodec.preferenceValue to false
            }
            transcodeOption.appendQueryParameters(
                this,
                videoCodecOverride = videoCodecOverride,
                audioCodecOverride = audioCodecOverride,
                videoCodecProfileOverride = videoProfileOverride,
                allowAudioStreamCopyOverride = allowAudioStreamCopyOverride,
            )
        }
    }

    private fun updateStartTimeTicks(currentUrl: String, positionMs: Long): String {
        val ticksValue = (positionMs.coerceAtLeast(0L) * 10_000).toString()
        return runCatching {
            val parsed = Uri.parse(currentUrl)
            val builder = parsed.buildUpon().clearQuery()
            parsed.queryParameterNames
                .filterNot { it.equals("StartTimeTicks", ignoreCase = true) }
                .forEach { name ->
                    parsed.getQueryParameters(name).forEach { value ->
                        builder.appendQueryParameter(name, value)
                    }
                }
            builder.appendQueryParameter("StartTimeTicks", ticksValue)
            builder.build().toString()
        }.getOrElse { currentUrl }
    }

    fun getExternalPlaybackUrl(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        mediaSourceId: String? = null,
        audioStreamIndex: Int? = null,
        subtitleStreamIndex: Int? = null
    ): String {
        val sourceId = mediaSourceId ?: mediaItem.mediaSources.firstOrNull()?.id ?: mediaItem.id
        val container = mediaItem.getStreamContainer(sourceId) ?: "mkv"
        return buildVideoUrl(
            serverUrl = serverUrl,
            accessToken = accessToken,
            mediaItem = mediaItem,
            mediaSourceId = sourceId,
            container = container,
            audioStreamIndex = audioStreamIndex,
            subtitleStreamIndex = subtitleStreamIndex
        )
    }

    fun updateAudioStream(
        audioStreamIndex: Int,
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        shouldRebuildUrl: Boolean = true
    ) {
        currentAudioStreamIndex = audioStreamIndex
        val selectedStream = _state.value.availableAudioStreams
            .firstOrNull { it.index == audioStreamIndex }

        _state.value = _state.value.copy(
            playbackOptions = _state.value.playbackOptions.copy(
                selectedAudioStream = selectedStream
            ),
            audioStreamIndex = currentAudioStreamIndex,
            preferredAudioLanguage = selectedStream?.language
        )

        viewModelScope.launch {
            preferencesManager.saveAudioTrackLanguage(selectedStream?.language)
            persistPlaybackPreferences()
        }

        if (shouldRebuildUrl) {
            rebuildUrl(mediaItem, serverUrl, accessToken)
        }
        viewModelScope.launch {
            _trackChangeEvents.emit(
                TrackChangeEvent(currentAudioStreamIndex, currentSubtitleStreamIndex)
            )
        }
    }

    fun updateSubtitleStream(
        subtitleStreamIndex: Int?,
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String,
        shouldRebuildUrl: Boolean = true
    ) {
        currentSubtitleStreamIndex = subtitleStreamIndex
        val selectedSubtitleStream = subtitleStreamIndex?.let { index ->
            _state.value.availableSubtitleStreams.firstOrNull { it.index == index }
        }

        val updatedOptions = _state.value.playbackOptions.copy(
            selectedSubtitleStream = selectedSubtitleStream
        )

        _state.value = _state.value.copy(
            playbackOptions = updatedOptions,
            subtitleStreamIndex = currentSubtitleStreamIndex,
            preferredSubtitleLanguage = selectedSubtitleStream?.language
        )

        viewModelScope.launch {
            preferencesManager.saveSubtitleLanguage(selectedSubtitleStream?.language)
            persistPlaybackPreferences()
        }

        if (shouldRebuildUrl) {
            rebuildUrl(mediaItem, serverUrl, accessToken)
        }
        viewModelScope.launch {
            _trackChangeEvents.emit(
                TrackChangeEvent(currentAudioStreamIndex, currentSubtitleStreamIndex)
            )
        }
    }

    fun selectPlaybackTranscodeOption(option: PlaybackTranscodeOption) {
        if (option == selectedTranscodeOption) return
        selectedTranscodeOption = option
        val currentState = _state.value
        _state.value = currentState.copy(
            selectedPlaybackTranscodeOption = selectedTranscodeOption,
            totalDurationSeconds = if (option.isOriginal) {
                currentState.mediaItem?.runTimeTicks?.div(10_000_000L)
            } else {
                currentState.totalDurationSeconds
            },
            playbackOffsetMs = if (option.isOriginal) 0 else currentState.playbackOffsetMs
        )
        val mediaItem = currentState.mediaItem ?: return
        val serverUrl = savedServerUrl ?: return
        val accessToken = savedAccessToken ?: return
        val currentUrl = currentState.videoUrl
        if (currentUrl != null && currentUrl.startsWith("file:")) {
            return
        }
        rebuildUrl(mediaItem, serverUrl, accessToken)
    }

    private fun rebuildUrl(
        mediaItem: MediaItem,
        serverUrl: String,
        accessToken: String
    ) {
        val container = mediaItem.getStreamContainer(currentMediaSourceId) ?: "mkv"
        val absolutePositionMs = if (selectedTranscodeOption.isOriginal) {
            _state.value.startPositionMs
        } else {
            max(
                _state.value.playbackOffsetMs,
                _state.value.lastSavedPosition * 1000
            )
        }
        val videoUrl = buildVideoUrl(
            serverUrl = serverUrl,
            accessToken = accessToken,
            mediaItem = mediaItem,
            mediaSourceId = currentMediaSourceId
                ?: mediaItem.mediaSources.firstOrNull()?.id
                ?: mediaItem.id,
            container = container,
            audioStreamIndex = currentAudioStreamIndex,
            subtitleStreamIndex = currentSubtitleStreamIndex,
            transcodeOption = selectedTranscodeOption,
            startPositionMs = if (selectedTranscodeOption.isOriginal) null else absolutePositionMs
        )

        _state.value = _state.value.copy(
            videoUrl = videoUrl,
            mediaSourceId = currentMediaSourceId,
            audioStreamIndex = currentAudioStreamIndex,
            subtitleStreamIndex = currentSubtitleStreamIndex,
            selectedPlaybackTranscodeOption = selectedTranscodeOption
        )

        if (!selectedTranscodeOption.isOriginal) {
            updateDurationFromApi(mediaItem)
        }
    }

    private fun rebuildWithCurrentSelection() {
        val mediaItem = _state.value.mediaItem ?: return
        val serverUrl = savedServerUrl ?: return
        val accessToken = savedAccessToken ?: return
        rebuildUrl(mediaItem, serverUrl, accessToken)
    }

    private fun updateDurationFromApi(mediaItem: MediaItem) {
        val userId = savedUserId ?: return
        val accessToken = savedAccessToken ?: return
        viewModelScope.launch {
            when (val result = libraryRepository.getMediaItemDetail(userId, mediaItem.id, accessToken)) {
                is NetworkResult.Success -> {
                    val refreshedTicks = result.data.runTimeTicks ?: mediaItem.runTimeTicks
                    val currentItem = _state.value.mediaItem ?: mediaItem
                    val updatedItem = currentItem.copy(runTimeTicks = refreshedTicks)
                    val durationSeconds = refreshedTicks?.div(10_000_000L)
                    _state.value = _state.value.copy(
                        mediaItem = updatedItem,
                        totalDurationSeconds = durationSeconds
                    )
                }

                is NetworkResult.Error -> {
                    Log.w(
                        "VideoPlaybackViewModel",
                        "Failed to refresh playback duration: ${result.message}",
                        result.exception
                    )
                }

                else -> Unit
            }
        }
    }

    fun updatePlaybackPosition(positionMs: Long) {
        val positionTicks = positionMs * 10_000
        val newSegment = _state.value.segments.firstOrNull {
            positionTicks >= it.startPositionTicks && positionTicks < it.endPositionTicks
        }
        val positionSeconds = positionMs / 1000
        val currentState = _state.value
        if (newSegment?.id != currentState.activeSegment?.id || currentState.lastSavedPosition != positionSeconds) {
            _state.value = currentState.copy(
                activeSegment = newSegment,
                lastSavedPosition = positionSeconds
            )
        }
    }

    fun skipSegment() {
        val segment = _state.value.activeSegment ?: return
        val endMs = segment.endPositionTicks / 10_000
        handleSeekRequest(endMs)
        _state.value = _state.value.copy(activeSegment = null)
    }

    fun handleSeekRequest(positionMs: Long): Boolean {
        val mediaItem = _state.value.mediaItem
        val serverUrl = savedServerUrl
        val accessToken = savedAccessToken
        val nextUpdateCount = _state.value.startPositionUpdateCount + 1
        return if (
            mediaItem != null &&
            !selectedTranscodeOption.isOriginal &&
            serverUrl != null &&
            accessToken != null
        ) {
            val container = mediaItem.getStreamContainer(currentMediaSourceId) ?: "mkv"
            val existingUrl = _state.value.videoUrl
            val videoUrl = if (existingUrl.isNullOrBlank()) {
                buildVideoUrl(
                    serverUrl = serverUrl,
                    accessToken = accessToken,
                    mediaItem = mediaItem,
                    mediaSourceId = currentMediaSourceId
                        ?: mediaItem.mediaSources.firstOrNull()?.id
                        ?: mediaItem.id,
                    container = container,
                    audioStreamIndex = currentAudioStreamIndex,
                    subtitleStreamIndex = currentSubtitleStreamIndex,
                    transcodeOption = selectedTranscodeOption,
                    startPositionMs = positionMs
                )
            } else {
                updateStartTimeTicks(existingUrl, positionMs)
            }
            _state.value = _state.value.copy(
                videoUrl = videoUrl,
                exoMediaItem = null,
                cacheDataSourceFactory = null,
                mediaSourceId = currentMediaSourceId,
                audioStreamIndex = currentAudioStreamIndex,
                subtitleStreamIndex = currentSubtitleStreamIndex,
                startPositionMs = 0,
                playbackOffsetMs = positionMs,
                startPositionUpdateCount = nextUpdateCount,
                lastSavedPosition = positionMs / 1000
            )
            true
        } else {
            _state.value = _state.value.copy(
                startPositionMs = positionMs,
                playbackOffsetMs = 0,
                startPositionUpdateCount = nextUpdateCount,
                lastSavedPosition = positionMs / 1000
            )
            false
        }
    }

    fun savePlaybackPosition(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionSeconds: Long
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val positionTicks = positionSeconds * 10_000_000
                mediaItemDao.updatePlaybackPosition(mediaId, userId, positionTicks)
                preferencesManager.savePlaybackPosition(mediaId, positionTicks)
                val isOnline = isConnected.value
                val result = if (isOnline) {
                    libraryRepository.reportPlaybackProgress(
                        mediaId = mediaId,
                        userId = userId,
                        accessToken = accessToken,
                        positionTicks = positionTicks
                    )
                } else {
                    NetworkResult.Error("No network connection")
                }
                val download = downloadDao.getDownloadByMediaId(mediaId)
                if (download != null) {
                    val synced = isOnline && result is NetworkResult.Success
                    playbackLogDao.upsert(
                        PlaybackLogEntity(
                            mediaId = mediaId,
                            positionTicks = positionTicks,
                            isCompleted = false,
                            isSynced = synced
                        )
                    )
                    if (!synced) {
                        PlaybackLogSyncWorker.enqueue(context)
                    }
                }

                _state.value = _state.value.copy(lastSavedPosition = positionSeconds)
            } catch (e: Exception) {
                println("Error saving playback position: ${e.message}")
            }
        }
    }

    fun markAsWatched(
        mediaId: String,
        userId: String
    ) {
        viewModelScope.launch {
            try {
                mediaItemDao.updatePlayedStatus(
                    mediaId, userId,
                    played = true,
                    pendingPlayed = true
                )
                preferencesManager.clearPlaybackPosition(mediaId)
                _state.value = _state.value.copy(isMarkedAsWatched = true)
            } catch (e: Exception) {
                println("Error marking as watched: ${e.message}")
            }
        }
    }

    fun reportPlaybackStart(mediaId: String, userId: String, accessToken: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                libraryRepository.reportPlaybackStart(mediaId, userId, accessToken)
                _state.value = _state.value.copy(playbackStartReported = true)
            } catch (e: Exception) {
                println("Error reporting playback start: ${e.message}")
            }
        }
    }

    fun reportPlaybackStop(
        mediaId: String,
        userId: String,
        accessToken: String,
        positionSeconds: Long,
        isCompleted: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val positionTicks = positionSeconds * 10_000_000
                val finalTicks = if (isCompleted) 0L else positionTicks

                if (isCompleted) {
                    mediaItemDao.updatePlaybackPosition(mediaId, userId, 0)
                    preferencesManager.clearPlaybackPosition(mediaId)
                } else {
                    mediaItemDao.updatePlaybackPosition(mediaId, userId, finalTicks)
                    preferencesManager.savePlaybackPosition(mediaId, finalTicks)
                }

                val result = libraryRepository.reportPlaybackStop(
                    mediaId = mediaId,
                    userId = userId,
                    accessToken = accessToken,
                    positionTicks = finalTicks
                )

                val download = downloadDao.getDownloadByMediaId(mediaId)
                if (download != null) {
                    val synced = result is NetworkResult.Success
                    playbackLogDao.upsert(
                        PlaybackLogEntity(
                            mediaId = mediaId,
                            positionTicks = finalTicks,
                            isCompleted = isCompleted,
                            isSynced = synced
                        )
                    )
                    if (!synced) {
                        PlaybackLogSyncWorker.enqueue(context)
                    }
                }

                _state.value = _state.value.copy(playbackStopReported = true)
            } catch (e: Exception) {
                println("Error reporting playback stop: ${e.message}")
            }
        }
    }

    fun updateBufferingState(isBuffering: Boolean) {
        _state.value = _state.value.copy(isBuffering = isBuffering)
    }

    fun updatePlaybackSpeed(speed: Float) {
        _state.value = _state.value.copy(playbackSpeed = speed)
    }

    fun updateVolume(volume: Long) {
        _state.value = _state.value.copy(volume = volume.coerceIn(0, 100))
    }

    fun updatePausedState(isPaused: Boolean) {
        _state.value = _state.value.copy(isPaused = isPaused)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun reset() {
        _state.value = VideoPlaybackState()
        currentMediaSourceId = null
        currentAudioStreamIndex = null
        currentSubtitleStreamIndex = null
    }
}