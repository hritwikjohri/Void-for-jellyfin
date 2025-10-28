package com.hritwik.avoid.presentation.viewmodel.user

import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.hritwik.avoid.core.ServiceEventBus
import com.hritwik.avoid.core.ServiceManager
import com.hritwik.avoid.data.cache.CacheManager
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.common.RepositoryCache
import com.hritwik.avoid.data.download.DownloadCoordinator
import com.hritwik.avoid.data.download.DownloadService
import com.hritwik.avoid.data.download.toDownloadInfo
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.model.PlaybackPreferences
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.download.DownloadCodec
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.jellyseer.JellyseerConfig
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.domain.model.playback.PreferredAudioCodec
import com.hritwik.avoid.domain.model.playback.PreferredVideoCodec
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.jellyseer.LoginToJellyseerUseCase
import com.hritwik.avoid.domain.usecase.jellyseer.LogoutFromJellyseerUseCase
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import com.hritwik.avoid.utils.constants.PreferenceConstants
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import com.hritwik.avoid.utils.helpers.normalizeUuid
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Locale
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class UserDataViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val downloadDao: DownloadDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
    private val serviceManager: ServiceManager,
    private val downloadCoordinator: DownloadCoordinator,
    private val cacheManager: CacheManager,
    private val loginToJellyseerUseCase: LoginToJellyseerUseCase,
    private val logoutFromJellyseerUseCase: LogoutFromJellyseerUseCase,
    connectivityObserver: ConnectivityObserver
) : BaseViewModel(connectivityObserver) {

    private val cache = RepositoryCache()
    private val json = Json { ignoreUnknownKeys = true }

    private val _favorites = MutableStateFlow<List<MediaItem>>(emptyList())
    val favorites: StateFlow<List<MediaItem>> = _favorites.asStateFlow()
    private val _playedItems = MutableStateFlow<Set<String>>(emptySet())
    val playedItems: StateFlow<Set<String>> = _playedItems.asStateFlow()
    private val remotePlayedItems = MutableStateFlow<Set<String>>(emptySet())
    private var latestLocalPlayedItems: Set<String> = emptySet()
    private val _playedError = MutableStateFlow<AppError?>(null)

    private val _serviceEvents = MutableSharedFlow<ServiceEventBus.Event>()
    val serviceEvents: SharedFlow<ServiceEventBus.Event> = _serviceEvents.asSharedFlow()

    private var favoritesCollectionJob: Job? = null
    private var playedItemsCollectionJob: Job? = null

    private val _mpvConfig = MutableStateFlow("")
    val mpvConfig: StateFlow<String> = _mpvConfig.asStateFlow()

    init {
        viewModelScope.launch {
            ServiceEventBus.events.collect { _serviceEvents.emit(it) }
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { preferencesManager.getMpvConfig() }
                .onSuccess { config -> _mpvConfig.value = config }
                .onFailure { error ->
                    Log.e("UserDataViewModel", "Failed to load MPV config", error)
                }
        }
    }

    fun refreshMpvConfig() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { preferencesManager.getMpvConfig() }
                .onSuccess { config -> _mpvConfig.value = config }
                .onFailure { error ->
                    Log.e("UserDataViewModel", "Failed to refresh MPV config", error)
                }
        }
    }

    fun saveMpvConfig(config: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                preferencesManager.saveMpvConfig(config)
                config
            }
                .onSuccess { savedConfig -> _mpvConfig.value = savedConfig }
                .onFailure { error ->
                    Log.e("UserDataViewModel", "Failed to save MPV config", error)
                }
        }
    }

    fun isFavorite(mediaId: String): StateFlow<Boolean> =
        favorites
            .map { list -> list.any { it.id == mediaId } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun isPlayed(mediaId: String): StateFlow<Boolean> =
        _playedItems
            .map { it.contains(mediaId) }
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private fun updatePlayedItemsState() {
        _playedItems.value = latestLocalPlayedItems + remotePlayedItems.value
    }

    fun downloadStatus(id: String): StateFlow<DownloadService.DownloadStatus?> =
        downloads
            .map { list ->
                list.find { it.mediaSourceId == id || it.mediaItem.id == id }?.status
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val downloads: StateFlow<List<DownloadService.DownloadInfo>> =
        combine(
            downloadDao.getAllDownloads(),
            serviceManager.downloads
        ) { entities, managerMap ->
            val managerInfos = managerMap.values.toList()
            val remaining = entities
                .filter { (it.mediaSourceId ?: it.mediaId) !in managerMap.keys }
                .map { it.toDownloadInfo(json) }
            sortDownloads(managerInfos + remaining)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun sortDownloads(downloads: List<DownloadService.DownloadInfo>): List<DownloadService.DownloadInfo> {
        return downloads.sortedWith(
            compareBy(
                { it.showNameSortKey() },
                { it.seasonSortKey() },
                { it.episodeSortKey() },
                { it.mediaItem.name.lowercase(Locale.ROOT) }
            )
        )
    }

    private fun DownloadService.DownloadInfo.showNameSortKey(): String {
        val item = mediaItem
        val name = if (item.type.equals("Episode", ignoreCase = true)) {
            item.seriesName ?: item.name
        } else {
            item.name
        }
        return name.lowercase(Locale.ROOT)
    }

    private fun DownloadService.DownloadInfo.seasonSortKey(): Int {
        val item = mediaItem
        return if (item.type.equals("Episode", ignoreCase = true)) {
            item.parentIndexNumber ?: Int.MAX_VALUE
        } else {
            Int.MAX_VALUE
        }
    }

    private fun DownloadService.DownloadInfo.episodeSortKey(): Int {
        val item = mediaItem
        return if (item.type.equals("Episode", ignoreCase = true)) {
            item.indexNumber ?: Int.MAX_VALUE
        } else {
            Int.MAX_VALUE
        }
    }

    fun pauseAllDownloads() {
        viewModelScope.launch { serviceManager.pauseAllDownloads() }
    }

    fun resumeAllDownloads() {
        viewModelScope.launch { serviceManager.resumeAllDownloads() }
    }

    fun deleteDownloads(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id ->
                val candidates = buildIdCandidates(id)
                candidates.forEach { candidate -> serviceManager.cancelDownload(candidate) }
                val entity = findDownloadEntity(candidates)
                if (entity != null) {
                    runCatching { downloadDao.deleteDownload(entity) }
                }
            }
            serviceManager.cleanupOrphanedDownloads()
        }
    }

    private suspend fun findDownloadEntity(candidates: Set<String>) =
        candidates.firstNotNullOfOrNull { candidate ->
            downloadDao.getDownloadByMediaSourceId(candidate)
        } ?: candidates.firstNotNullOfOrNull { candidate ->
            downloadDao.getDownloadByMediaId(candidate)
        }

    private fun buildIdCandidates(id: String): Set<String> {
        val normalized = normalizeUuid(id)
        val compact = normalized.replace("-", "")
        return buildSet {
            add(id)
            add(normalized)
            add(compact)
        }
    }

    data class HomeSettings(
        val showFeaturedHeader: Boolean,
        val ambientBackground: Boolean,
        val navigateEpisodesToSeason: Boolean
    )

    val homeSettings: StateFlow<HomeSettings> =
        combine(
            preferencesManager.getShowFeaturedHeader(),
            preferencesManager.getAmbientBackgroundEnabled(),
            preferencesManager.getNavigateEpisodesToSeason()
        ) { featured, ambient, navigateEpisodesToSeason ->
            HomeSettings(featured, ambient, navigateEpisodesToSeason)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            HomeSettings(
                showFeaturedHeader = false,
                ambientBackground = true,
                navigateEpisodesToSeason = true
            )
        )

    data class PlaybackSettings(
        val playThemeSongs: Boolean,
        val displayMode: DisplayMode,
        val decoderMode: DecoderMode,
        val playerType: PlayerType,
        val preferredVideoCodec: PreferredVideoCodec,
        val preferredAudioCodec: PreferredAudioCodec,
        val autoPlayNextEpisode: Boolean,
        val autoSkipSegments: Boolean
    )

    val playbackSettings: StateFlow<PlaybackSettings> =
        combine(
            preferencesManager.getPlayThemeSongs(),
            preferencesManager.getDisplayMode(),
            preferencesManager.getDecoderMode(),
            preferencesManager.getPlayerType(),
            preferencesManager.getPreferredVideoCodec(),
            preferencesManager.getPreferredAudioCodec(),
            preferencesManager.getAutoPlay(),
            preferencesManager.getAutoSkipSegments()
        ) { values ->
            val playTheme = values[0] as Boolean
            val display = values[1] as DisplayMode
            val decoder = values[2] as DecoderMode
            val playerType = values[3] as PlayerType
            val videoCodec = values[4] as PreferredVideoCodec
            val audioCodec = values[5] as PreferredAudioCodec
            val autoPlayNext = values[6] as Boolean
            val autoSkip = values[7] as Boolean

            PlaybackSettings(
                playTheme,
                display,
                decoder,
                playerType,
                videoCodec,
                audioCodec,
                autoPlayNext,
                autoSkip
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PlaybackSettings(
                false,
                DisplayMode.FIT_SCREEN,
                DecoderMode.fromValue(PreferenceConstants.DEFAULT_DECODER_MODE),
                PlayerType.fromValue(PreferenceConstants.DEFAULT_PLAYER_TYPE),
                PreferredVideoCodec.fromPreferenceValue(PreferenceConstants.DEFAULT_PREFERRED_VIDEO_CODEC),
                PreferredAudioCodec.fromPreferenceValue(PreferenceConstants.DEFAULT_PREFERRED_AUDIO_CODEC),
                autoPlayNextEpisode = true,
                autoSkipSegments = false
            )
        )


    data class DownloadSettings(
        val downloadWifiOnly: Boolean,
        val downloadQuality: DownloadQuality,
        val downloadCodec: DownloadCodec,
        val autoDeleteDownloads: Boolean,
        val storageLocation: String,
    )

    private val defaultSettings = DownloadSettings(
        downloadWifiOnly = true,
        downloadQuality = DownloadQuality.FHD_1080,
        downloadCodec = DownloadCodec.fromLabel(PreferenceConstants.DEFAULT_DOWNLOAD_CODEC),
        autoDeleteDownloads = false,
        storageLocation = PreferenceConstants.DEFAULT_DOWNLOAD_LOCATION,
    )

    
    private val wifiOnlyFlow = preferencesManager.getDownloadWifiOnly()
        .onStart { emit(true) }
        .catch { emit(true) }

    private val qualityFlow = preferencesManager.getDownloadQuality()
        .map { label -> DownloadQuality.entries.firstOrNull { it.label == label } ?: DownloadQuality.FHD_1080 }
        .onStart { emit(DownloadQuality.FHD_1080) }
        .catch { emit(DownloadQuality.FHD_1080) }

    private val codecFlow = preferencesManager.getDownloadCodec()
        .map { DownloadCodec.fromLabel(it) }
        .onStart { emit(DownloadCodec.fromLabel(PreferenceConstants.DEFAULT_DOWNLOAD_CODEC)) }
        .catch { emit(DownloadCodec.H264) }

    private val autoDeleteFlow = preferencesManager.getAutoDeleteDownloads()
        .onStart { emit(false) }
        .catch { emit(false) }

    private val locationFlow = preferencesManager.getDownloadLocation()
        .onStart { emit(PreferenceConstants.DEFAULT_DOWNLOAD_LOCATION) }
        .catch { emit(PreferenceConstants.DEFAULT_DOWNLOAD_LOCATION) }

    val downloadSettings: StateFlow<DownloadSettings> =
        combine(
            wifiOnlyFlow,
            qualityFlow,
            codecFlow,
            autoDeleteFlow,
            locationFlow
        ) { wifiOnly, quality, codec, autoDel, location ->
            DownloadSettings(wifiOnly, quality, codec, autoDel, location)
        }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), defaultSettings)


    val availableDownloadQualities: List<DownloadQuality> = DownloadQuality.entries
    val availableDownloadCodecs: List<DownloadCodec> = DownloadCodec.entries

    data class PersonalizationSettings(
        val themeMode: String,
        val fontScale: Float,
        val language: String,
        val gesturesEnabled: Boolean,
        val highContrast: Boolean,
    )

    val personalizationSettings: StateFlow<PersonalizationSettings> =
        combine(
            preferencesManager.getThemeMode(),
            preferencesManager.getFontScale(),
            preferencesManager.getPreferredLanguage(),
            preferencesManager.getGestureControlsEnabled(),
            preferencesManager.getHighContrastEnabled(),
        ) { theme, scale, lang, gestures, contrast ->
            PersonalizationSettings(theme, scale, lang, gestures, contrast)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PersonalizationSettings(
                PreferenceConstants.DEFAULT_THEME_MODE,
                PreferenceConstants.DEFAULT_FONT_SCALE,
                PreferenceConstants.DEFAULT_PREFERRED_LANGUAGE,
                gesturesEnabled = true,
                highContrast = false,
            )
        )

    val jellyseerSettings: StateFlow<JellyseerConfig> =
        preferencesManager.getJellyseerConfig()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                JellyseerConfig()
            )

    data class JellyseerAuthUiState(
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
    )

    private val _jellyseerAuthState = MutableStateFlow(JellyseerAuthUiState())
    val jellyseerAuthState: StateFlow<JellyseerAuthUiState> = _jellyseerAuthState.asStateFlow()

    fun setShowFeaturedHeader(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveShowFeaturedHeader(enabled) }
    }

    fun getPlaybackPreferences(mediaId: String): Flow<PlaybackPreferences?> =
        preferencesManager.getPlaybackPreferences(mediaId)

    fun setAmbientBackgroundEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveAmbientBackgroundEnabled(enabled) }
    }

    fun setNavigateEpisodesToSeason(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveNavigateEpisodesToSeason(enabled) }
    }

    fun setPlayThemeSongs(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.savePlayThemeSongs(enabled) }
    }

    fun setDisplayMode(mode: DisplayMode) {
        viewModelScope.launch { preferencesManager.saveDisplayMode(mode) }
    }

    fun setDecoderMode(mode: DecoderMode) {
        viewModelScope.launch { preferencesManager.saveDecoderMode(mode) }
    }

    fun setPreferredVideoCodec(codec: PreferredVideoCodec) {
        viewModelScope.launch { preferencesManager.savePreferredVideoCodec(codec) }
    }

    fun setPreferredAudioCodec(codec: PreferredAudioCodec) {
        viewModelScope.launch { preferencesManager.savePreferredAudioCodec(codec) }
    }

    fun setPlayerType(playerType: PlayerType) {
        viewModelScope.launch { preferencesManager.savePlayerType(playerType) }
    }

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveAutoPlay(enabled) }
    }

    fun setAutoSkipSegments(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveAutoSkipSegments(enabled) }
    }

    fun updateJellyseerBaseUrl(url: String) {
        viewModelScope.launch {
            preferencesManager.updateJellyseerBaseUrl(url)
        }
    }

    fun updateJellyseerApiKey(apiKey: String) {
        viewModelScope.launch {
            preferencesManager.updateJellyseerApiKey(apiKey)
        }
    }

    fun loginToJellyseer(baseUrl: String, username: String, password: String) {
        if (_jellyseerAuthState.value.isLoading) return
        viewModelScope.launch {
            when {
                baseUrl.isBlank() -> {
                    _jellyseerAuthState.update {
                        it.copy(
                            errorMessage = "Base URL is required",
                            successMessage = null
                        )
                    }
                    return@launch
                }
                username.isBlank() -> {
                    _jellyseerAuthState.update {
                        it.copy(
                            errorMessage = "Username is required",
                            successMessage = null
                        )
                    }
                    return@launch
                }
            }

            _jellyseerAuthState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            when (val result = loginToJellyseerUseCase(
                LoginToJellyseerUseCase.Params(
                    baseUrl = baseUrl,
                    username = username,
                    password = password,
                )
            )) {
                is NetworkResult.Success -> {
                    val displayName = result.data.displayName
                        ?: result.data.username
                        ?: result.data.email
                        ?: username

                    _jellyseerAuthState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Signed in as $displayName",
                            errorMessage = null
                        )
                    }
                }

                is NetworkResult.Error -> {
                    _jellyseerAuthState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            successMessage = null
                        )
                    }
                }

                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun logoutOfJellyseer() {
        if (_jellyseerAuthState.value.isLoading) return
        viewModelScope.launch {
            _jellyseerAuthState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }

            when (val result = logoutFromJellyseerUseCase()) {
                is NetworkResult.Success -> {
                    _jellyseerAuthState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Signed out of Jellyseer",
                            errorMessage = null
                        )
                    }
                }

                is NetworkResult.Error -> {
                    _jellyseerAuthState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            successMessage = null
                        )
                    }
                }

                is NetworkResult.Loading -> Unit
            }
        }
    }

    fun clearJellyseerAuthFeedback() {
        _jellyseerAuthState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun setGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveGestureControlsEnabled(enabled) }
    }

    fun setDownloadWifiOnly(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveDownloadWifiOnly(enabled) }
    }

    fun setDownloadQuality(quality: DownloadQuality) {
        viewModelScope.launch { preferencesManager.saveDownloadQuality(quality.label) }
    }

    fun setDownloadCodec(codec: DownloadCodec) {
        viewModelScope.launch { preferencesManager.saveDownloadCodec(codec.preferenceValue) }
    }

    fun setStorageLocation(location: String) {
        viewModelScope.launch {
            preferencesManager.saveDownloadLocation(location)
        }
    }

    fun clearCache(): Boolean {
        cacheManager.clearAll()
        return true
    }

    fun clearDownloads() {
        viewModelScope.launch(Dispatchers.IO) {
            val activeIds = serviceManager.downloads.value.keys.toList()
            activeIds.forEach { id ->
                runCatching { serviceManager.cancelDownload(id) }
            }

            val storedDownloads = downloadDao.getAllDownloads().first()

            storedDownloads.forEach { entity ->
                entity.filePath?.let { path ->
                    val parent = File(path).parentFile
                    if (parent?.exists() == true) {
                        deleteDir(parent, "Download file directory")
                    }
                }

                entity.subtitleFilePaths.forEach { subtitlePath ->
                    val subtitleFile = File(subtitlePath)
                    if (subtitleFile.exists()) {
                        if (subtitleFile.isDirectory) {
                            deleteDir(subtitleFile, "Subtitle directory")
                        } else {
                            subtitleFile.delete()
                        }
                    }
                }

                entity.mediaSourceId?.let { sourceId ->
                    val tempDir = File(context.filesDir, "downloads/$sourceId")
                    if (tempDir.exists()) {
                        deleteDir(tempDir, "Download temp directory")
                    }
                }
            }

            val cacheDownloadsDir = File(context.cacheDir, "downloads")
            if (cacheDownloadsDir.exists()) {
                deleteDir(cacheDownloadsDir, "Download cache directory")
            }

            val appDownloadsDir = File(context.filesDir, "downloads")
            if (appDownloadsDir.exists()) {
                deleteDir(appDownloadsDir, "Downloads directory")
            }

            context.getExternalFilesDir(null)?.let { externalFilesDir ->
                val externalDownloadsDir = File(externalFilesDir, "downloads")
                if (externalDownloadsDir.exists()) {
                    deleteDir(externalDownloadsDir, "External downloads directory")
                }
            }

            downloadDao.deleteAllDownloads()
        }
    }

    fun loadPlayedItems(userId: String, accessToken: String) {
        playedItemsCollectionJob?.cancel()
        playedItemsCollectionJob = viewModelScope.launch {
            libraryRepository.getPlayedItems(userId).collectLatest { items ->
                latestLocalPlayedItems = items.map { it.id }.toSet()
                updatePlayedItemsState()
            }
        }

        viewModelScope.launch {
            when (val result = cache.get("played_$userId") {
                libraryRepository.getWatchHistory(userId, accessToken, 100)
            }) {
                is NetworkResult.Success -> {
                    remotePlayedItems.value = result.data.map { it.id }.toSet()
                    updatePlayedItemsState()
                    _playedError.value = null
                }
                is NetworkResult.Error -> _playedError.value = result.error
                else -> Unit
            }
        }
    }

    fun loadFavorites(userId: String, accessToken: String) {
        favoritesCollectionJob?.cancel()
        favoritesCollectionJob = viewModelScope.launch {
            libraryRepository.getFavoriteItems(userId).collectLatest { items ->
                _favorites.value = items
            }
        }

        
        viewModelScope.launch {
            when (cache.get("favorites_$userId") {
                libraryRepository.getFavoriteItems(userId, accessToken, 50)
            }) {
                is NetworkResult.Error -> Unit
                else -> Unit
            }
        }
    }

    fun toggleFavorite(
        userId: String,
        mediaId: String,
        accessToken: String,
        isFavorite: Boolean,
        mediaItem: MediaItem? = null
    ) {
        val newFavorite = !isFavorite
        viewModelScope.launch {
            val previousFavorites = _favorites.value

            val item = if (newFavorite) {
                mediaItem
                    ?: libraryRepository.getMediaItemDetailLocal(userId, mediaId)
                    ?: when (val res = cache.get("detail_$mediaId") {
                        libraryRepository.getMediaItemDetail(userId, mediaId, accessToken)
                    }) {
                        is NetworkResult.Success -> res.data
                        else -> null
                    }
            } else {
                null
            }

            _favorites.value = if (newFavorite) {
                item?.let { previousFavorites + it } ?: previousFavorites
            } else {
                previousFavorites.filterNot { it.id == mediaId }
            }

            when (libraryRepository.toggleFavorite(userId, mediaId, accessToken, newFavorite, mediaItem)) {
                is NetworkResult.Success -> {
                    cache.invalidate("favorites_$userId")
                    loadFavorites(userId, accessToken)
                }

                is NetworkResult.Error -> {
                    _favorites.value = previousFavorites
                }

                is NetworkResult.Loading<*> -> TODO()
            }
        }
    }
    fun markAsPlayed(userId: String, mediaId: String, accessToken: String, played: Boolean) {
        viewModelScope.launch {
            val previousLocal = latestLocalPlayedItems
            val previousRemote = remotePlayedItems.value
            val previousCombined = _playedItems.value

            latestLocalPlayedItems = if (played) {
                latestLocalPlayedItems + mediaId
            } else {
                latestLocalPlayedItems - mediaId
            }
            remotePlayedItems.value = if (played) {
                previousRemote + mediaId
            } else {
                previousRemote - mediaId
            }
            updatePlayedItemsState()
            try {
                when (val result = libraryRepository.markAsPlayed(userId, mediaId, accessToken, played)) {
                    is NetworkResult.Success -> {
                        cache.invalidate("played_$userId")
                        loadPlayedItems(userId, accessToken)
                        _playedError.value = null
                    }
                    is NetworkResult.Error -> {
                        latestLocalPlayedItems = previousLocal
                        remotePlayedItems.value = previousRemote
                        _playedItems.value = previousCombined
                        _playedError.value = result.error
                    }
                    else -> Unit
                }
            } catch (e: Exception) {
                latestLocalPlayedItems = previousLocal
                remotePlayedItems.value = previousRemote
                _playedItems.value = previousCombined
                _playedError.value = AppError.Unknown(e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun startDownload(
        mediaItem: MediaItem,
        request: DownloadRequest,
        serverUrl: String,
        accessToken: String,
        priority: Int = 0,
        mediaSourceId: String? = null,
        userId: String? = null,
    ) {
        viewModelScope.launch {
            downloadCoordinator.startDownload(
                mediaItem = mediaItem,
                request = request,
                serverUrl = serverUrl,
                accessToken = accessToken,
                priority = priority,
                mediaSourceId = mediaSourceId,
                userId = userId
            )
        }
    }

    fun clearUserData() {
        viewModelScope.launch {
            preferencesManager.clearAllPreferences()
        }
    }

    fun reset() {
        favoritesCollectionJob?.cancel()
        favoritesCollectionJob = null
        playedItemsCollectionJob?.cancel()
        playedItemsCollectionJob = null
        _favorites.value = emptyList()
        _playedItems.value = emptySet()
        remotePlayedItems.value = emptySet()
        latestLocalPlayedItems = emptySet()
        _playedError.value = null
    }

    companion object {
        private const val TAG = "UserDataViewModel"

        fun deleteDir(dir: File, description: String): Boolean {
            val result = dir.deleteRecursively()
            if (result) {
                Log.d(TAG, "$description deleted: \${dir.absolutePath}")
            } else {
                Log.w(TAG, "Failed to delete $description: \${dir.absolutePath}")
            }
            return result
        }
    }
}