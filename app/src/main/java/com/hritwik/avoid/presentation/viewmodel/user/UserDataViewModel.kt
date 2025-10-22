package com.hritwik.avoid.presentation.viewmodel.user

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.work.WorkManager
import com.hritwik.avoid.core.ServiceEventBus
import com.hritwik.avoid.core.ServiceManager
import com.hritwik.avoid.data.cache.CacheManager
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.common.RepositoryCache
import com.hritwik.avoid.data.download.DownloadService
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.data.local.database.dao.DownloadDao
import com.hritwik.avoid.data.local.database.entities.DownloadEntity
import com.hritwik.avoid.data.local.model.PlaybackPreferences
import com.hritwik.avoid.data.sync.PlaybackLogSyncWorker
import com.hritwik.avoid.data.sync.UserDataSyncWorker
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.download.DownloadCodec
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.jellyseer.JellyseerConfig
import com.hritwik.avoid.domain.usecase.jellyseer.LoginToJellyseerUseCase
import com.hritwik.avoid.domain.usecase.jellyseer.LogoutFromJellyseerUseCase
import com.hritwik.avoid.domain.model.playback.DecoderMode
import com.hritwik.avoid.domain.model.playback.DisplayMode
import com.hritwik.avoid.domain.model.playback.PlayerType
import com.hritwik.avoid.domain.model.playback.PreferredAudioCodec
import com.hritwik.avoid.domain.model.playback.PreferredVideoCodec
import com.hritwik.avoid.domain.repository.LibraryRepository
import com.hritwik.avoid.domain.usecase.backup.BackupUseCase
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import com.hritwik.avoid.utils.constants.PreferenceConstants
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject

@OptIn(UnstableApi::class)
@HiltViewModel
class UserDataViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val downloadDao: DownloadDao,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
    private val serviceManager: ServiceManager,
    private val cacheManager: CacheManager,
    private val backupUseCase: BackupUseCase,
    private val dataWiper: com.hritwik.avoid.utils.DataWiper,
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
    val playedError: StateFlow<AppError?> = _playedError.asStateFlow()

    private val _serviceEvents = MutableSharedFlow<ServiceEventBus.Event>()
    val serviceEvents: SharedFlow<ServiceEventBus.Event> = _serviceEvents.asSharedFlow()

    private var favoritesCollectionJob: Job? = null
    private var playedItemsCollectionJob: Job? = null

    init {
        viewModelScope.launch {
            ServiceEventBus.events.collect { _serviceEvents.emit(it) }
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
                .map { it.toDownloadInfo() }
            managerInfos + remaining
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private fun DownloadEntity.toDownloadInfo(): DownloadService.DownloadInfo {
        val file = filePath?.let { File(it) } ?: File("")
        val dir = file.parentFile
        val source = mediaSourceId ?: mediaId
        val jsonFile = dir?.resolve("$source.json")
        val mediaItem = if (jsonFile?.exists() == true) {
            runCatching { json.decodeFromString<MediaItem>(jsonFile.readText()) }.getOrNull()
        } else null
        val finalItem = mediaItem ?: MediaItem(
            id = mediaId,
            name = title,
            title = title,
            type = type,
            overview = null,
            year = null,
            communityRating = null,
            runTimeTicks = null,
            primaryImageTag = null,
            thumbImageTag = null,
            logoImageTag = null,
            backdropImageTags = emptyList(),
            genres = emptyList(),
            isFolder = false,
            childCount = null,
            userData = null
        )
        val statusEnum = runCatching { DownloadService.DownloadStatus.valueOf(status) }
            .getOrElse { DownloadService.DownloadStatus.COMPLETED }
        val qualityEnum = runCatching { DownloadQuality.valueOf(quality) }
            .getOrElse { DownloadQuality.FHD_1080 }
        val parsedUri = runCatching { Uri.parse(requestUri) }.getOrNull()
        fun String?.toBooleanStrictOrDefault(default: Boolean): Boolean =
            this?.equals("true", ignoreCase = true) ?: default

        val codecParam = parsedUri?.getQueryParameter("VideoCodec")
        val codec = codecParam?.let { DownloadCodec.fromLabel(it) } ?: DownloadCodec.H264
        val request = DownloadRequest(
            quality = qualityEnum,
            static = parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true),
            maxWidth = parsedUri?.getQueryParameter("MaxWidth")?.toIntOrNull(),
            maxHeight = parsedUri?.getQueryParameter("MaxHeight")?.toIntOrNull(),
            maxBitrate = parsedUri?.getQueryParameter("MaxBitrate")?.toIntOrNull(),
            videoBitrate = parsedUri?.getQueryParameter("VideoBitrate")?.toIntOrNull(),
            audioBitrate = parsedUri?.getQueryParameter("AudioBitrate")?.toIntOrNull(),
            videoCodec = codec,
            audioCodec = parsedUri?.getQueryParameter("AudioCodec") ?: DownloadRequest.defaultAudioCodecs(),
            copySubtitles = parsedUri?.getQueryParameter("CopySubtitles").toBooleanStrictOrDefault(false),
            copyFontData = parsedUri?.getQueryParameter("CopyFonts").toBooleanStrictOrDefault(
                parsedUri?.getQueryParameter("EnableSubtitlesInManifest").toBooleanStrictOrDefault(false)
            ),
            enableAutoStreamCopy = parsedUri?.getQueryParameter("EnableAutoStreamCopy")
                .toBooleanStrictOrDefault(parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true)),
            allowVideoStreamCopy = parsedUri?.getQueryParameter("AllowVideoStreamCopy")
                .toBooleanStrictOrDefault(parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true)),
            allowAudioStreamCopy = parsedUri?.getQueryParameter("AllowAudioStreamCopy")
                .toBooleanStrictOrDefault(parsedUri?.getQueryParameter("static").toBooleanStrictOrDefault(true)),
        )
        return DownloadService.DownloadInfo(
            mediaItem = finalItem,
            requestUri = requestUri,
            file = file,
            progress = progress,
            status = statusEnum,
            serverUrl = serverUrl,
            accessToken = accessToken,
            priority = priority,
            addedAt = addedAt,
            quality = qualityEnum,
            request = request,
            mediaSourceId = mediaSourceId
        )
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
                PreferenceConstants.DEFAULT_SHOW_FEATURED_HEADER,
                PreferenceConstants.DEFAULT_AMBIENT_BACKGROUND,
                PreferenceConstants.DEFAULT_NAVIGATE_EPISODES_TO_SEASON
            )
        )

    data class PlaybackSettings(
        val playThemeSongs: Boolean,
        val displayMode: DisplayMode,
        val decoderMode: DecoderMode,
        val playerType: PlayerType,
        val preferredVideoCodec: PreferredVideoCodec,
        val preferredAudioCodec: PreferredAudioCodec,
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
            preferencesManager.getAutoSkipSegments()
        ) { values ->
            val playTheme = values[0] as Boolean
            val display = values[1] as DisplayMode
            val decoder = values[2] as DecoderMode
            val playerType = values[3] as PlayerType
            val videoCodec = values[4] as PreferredVideoCodec
            val audioCodec = values[5] as PreferredAudioCodec
            val autoSkip = values[6] as Boolean

            PlaybackSettings(
                playTheme,
                display,
                decoder,
                playerType,
                videoCodec,
                audioCodec,
                autoSkip
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            PlaybackSettings(
                PreferenceConstants.DEFAULT_PLAY_THEME_SONGS,
                DisplayMode.FIT_SCREEN,
                DecoderMode.fromValue(PreferenceConstants.DEFAULT_DECODER_MODE),
                PlayerType.fromValue(PreferenceConstants.DEFAULT_PLAYER_TYPE),
                PreferredVideoCodec.fromPreferenceValue(PreferenceConstants.DEFAULT_PREFERRED_VIDEO_CODEC),
                PreferredAudioCodec.fromPreferenceValue(PreferenceConstants.DEFAULT_PREFERRED_AUDIO_CODEC),
                PreferenceConstants.DEFAULT_AUTO_SKIP_SEGMENTS
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
        downloadWifiOnly = PreferenceConstants.DEFAULT_DOWNLOAD_WIFI_ONLY,
        downloadQuality = DownloadQuality.FHD_1080,
        downloadCodec = DownloadCodec.fromLabel(PreferenceConstants.DEFAULT_DOWNLOAD_CODEC),
        autoDeleteDownloads = PreferenceConstants.DEFAULT_AUTO_DELETE_DOWNLOADS,
        storageLocation = PreferenceConstants.DEFAULT_DOWNLOAD_LOCATION,
    )

    
    private val wifiOnlyFlow = preferencesManager.getDownloadWifiOnly()
        .onStart { emit(PreferenceConstants.DEFAULT_DOWNLOAD_WIFI_ONLY) }
        .catch { emit(PreferenceConstants.DEFAULT_DOWNLOAD_WIFI_ONLY) }

    private val qualityFlow = preferencesManager.getDownloadQuality()
        .map { label -> DownloadQuality.entries.firstOrNull { it.label == label } ?: DownloadQuality.FHD_1080 }
        .onStart { emit(DownloadQuality.FHD_1080) }
        .catch { emit(DownloadQuality.FHD_1080) }

    private val codecFlow = preferencesManager.getDownloadCodec()
        .map { DownloadCodec.fromLabel(it) }
        .onStart { emit(DownloadCodec.fromLabel(PreferenceConstants.DEFAULT_DOWNLOAD_CODEC)) }
        .catch { emit(DownloadCodec.H264) }

    private val autoDeleteFlow = preferencesManager.getAutoDeleteDownloads()
        .onStart { emit(PreferenceConstants.DEFAULT_AUTO_DELETE_DOWNLOADS) }
        .catch { emit(PreferenceConstants.DEFAULT_AUTO_DELETE_DOWNLOADS) }

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

    data class CacheSettings(
        val imageCacheSize: Long,
        val videoCacheSize: Long
    )

    val cacheSettings: StateFlow<CacheSettings> =
        combine(
            preferencesManager.getImageCacheSize(),
            preferencesManager.getVideoCacheSize()
        ) { image, video ->
            CacheSettings(image, video)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            CacheSettings(
                PreferenceConstants.DEFAULT_IMAGE_CACHE_SIZE.toLong(),
                PreferenceConstants.DEFAULT_VIDEO_CACHE_SIZE.toLong()
            )
        )

    data class BackgroundSettings(
        val syncEnabled: Boolean,
        val heartbeatEnabled: Boolean,
        val cleanupEnabled: Boolean,
    )

    val backgroundSettings: StateFlow<BackgroundSettings> =
        combine(
            preferencesManager.getSyncEnabled(),
            preferencesManager.getHeartbeatEnabled(),
            preferencesManager.getCleanupEnabled(),
        ) { sync, heartbeat, cleanup ->
            BackgroundSettings(sync, heartbeat, cleanup)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            BackgroundSettings(
                PreferenceConstants.DEFAULT_SYNC_ENABLED,
                PreferenceConstants.DEFAULT_HEARTBEAT_ENABLED,
                PreferenceConstants.DEFAULT_CLEANUP_ENABLED,
            )
        )

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
                PreferenceConstants.DEFAULT_GESTURE_CONTROLS,
                PreferenceConstants.DEFAULT_HIGH_CONTRAST,
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

    fun setAutoSkipSegments(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveAutoSkipSegments(enabled) }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesManager.saveThemeMode(mode)
            val nightMode = if (mode == "light")
                AppCompatDelegate.MODE_NIGHT_NO
            else
                AppCompatDelegate.MODE_NIGHT_YES
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }
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
                password.isBlank() -> {
                    _jellyseerAuthState.update {
                        it.copy(
                            errorMessage = "Password is required",
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

    fun setFontScale(scale: Float) {
        viewModelScope.launch { preferencesManager.saveFontScale(scale) }
    }

    fun setPreferredLanguage(language: String) {
        viewModelScope.launch { preferencesManager.savePreferredLanguage(language) }
    }

    fun setGesturesEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveGestureControlsEnabled(enabled) }
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveHighContrastEnabled(enabled) }
    }

    fun setSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesManager.saveSyncEnabled(enabled)
            if (enabled) {
                UserDataSyncWorker.enqueue(context)
                PlaybackLogSyncWorker.enqueue(context)
            } else {
                UserDataSyncWorker.cancel(context)
                WorkManager.getInstance(context).cancelUniqueWork("PlaybackLogSync")
            }
        }
    }

    fun setHeartbeatEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveHeartbeatEnabled(enabled) }
    }

    fun setCleanupEnabled(enabled: Boolean) {
        viewModelScope.launch { preferencesManager.saveCleanupEnabled(enabled) }
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

    fun clearImageCache(): Boolean {
        cacheManager.clearImageCache()
        return true
    }

    fun clearVideoCache(): Boolean {
        cacheManager.clearVideoCache()
        return true
    }

    fun clearDownloads(): Boolean {
        return deleteDir(File(context.cacheDir, "downloads"), "Downloads directory")
    }

    fun setImageCacheSize(sizeMb: Long) {
        viewModelScope.launch { cacheManager.setImageCacheSize(sizeMb) }
    }

    fun setVideoCacheSize(sizeMb: Long) {
        viewModelScope.launch { cacheManager.setVideoCacheSize(sizeMb) }
    }

    fun getImageCacheUsage(): Long = cacheManager.getImageCacheUsage()

    fun getVideoCacheUsage(): Long = cacheManager.getVideoCacheUsage()

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

            when (val result = libraryRepository.toggleFavorite(userId, mediaId, accessToken, newFavorite, mediaItem)) {
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
    ) {
        serviceManager.startDownload(
            mediaItem = mediaItem,
            serverUrl = serverUrl,
            accessToken = accessToken,
            request = request,
            priority = priority,
            mediaSourceId = mediaSourceId
        )
    }

    fun pauseDownload(id: String) {
        viewModelScope.launch {
            downloadDao.getDownloadByMediaSourceId(id)?.let {
                downloadDao.updateDownload(it.copy(status = "paused"))
            }
        }
    }

    fun resumeDownload(id: String) {
        viewModelScope.launch {
            downloadDao.getDownloadByMediaSourceId(id)?.let {
                downloadDao.updateDownload(it.copy(status = "downloading"))
            }
        }
    }

    fun cancelDownload(id: String) {
        viewModelScope.launch {
            serviceManager.cancelDownload(id)
            downloadDao.getDownloadByMediaSourceId(id)?.let {
                downloadDao.deleteDownload(it)
            }
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

    fun exportPersonalData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupUseCase(BackupUseCase.Params(BackupUseCase.Action.BACKUP))
            onResult(result is NetworkResult.Success)
        }
    }

    fun erasePersonalData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = runCatching { dataWiper.wipeAll() }
            onResult(result.isSuccess)
        }
    }

    fun backupAppData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupUseCase(BackupUseCase.Params(BackupUseCase.Action.BACKUP))
            onResult(result is NetworkResult.Success)
        }
    }

    fun restoreAppData(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = backupUseCase(BackupUseCase.Params(BackupUseCase.Action.RESTORE))
            onResult(result is NetworkResult.Success)
        }
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