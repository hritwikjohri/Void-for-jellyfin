package com.hritwik.avoid.presentation.viewmodel.media

import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.data.local.PreferencesManager
import com.hritwik.avoid.domain.usecase.library.GetMediaItemDetailLocalUseCase
import com.hritwik.avoid.domain.usecase.media.GetEpisodesUseCase
import com.hritwik.avoid.domain.usecase.media.GetMediaItemDetailUseCase
import com.hritwik.avoid.domain.usecase.media.GetSeasonsUseCase
import com.hritwik.avoid.domain.usecase.media.GetRelatedResourcesBatchUseCase
import com.hritwik.avoid.domain.usecase.media.GetThemeSongsUseCase
import com.hritwik.avoid.domain.usecase.media.GetThemeSongIdsUseCase
import com.hritwik.avoid.data.common.RepositoryCache
import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.presentation.ui.state.MediaDetailState
import com.hritwik.avoid.presentation.ui.state.ThemeSongState
import com.hritwik.avoid.presentation.ui.screen.player.ThemeSongController
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import com.hritwik.avoid.utils.constants.AppConstants.RESUME_COMPLETION_THRESHOLD
import com.hritwik.avoid.domain.model.library.UserData
import com.hritwik.avoid.data.connection.ServerConnectionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject

@HiltViewModel
class MediaViewModel @Inject constructor(
    private val getMediaItemDetailLocalUseCase: GetMediaItemDetailLocalUseCase,
    private val getMediaItemDetailUseCase: GetMediaItemDetailUseCase,
    private val getRelatedResourcesBatchUseCase: GetRelatedResourcesBatchUseCase,
    private val getSeasonsUseCase: GetSeasonsUseCase,
    private val getEpisodesUseCase: GetEpisodesUseCase,
    private val getThemeSongsUseCase: GetThemeSongsUseCase,
    private val getThemeSongIdsUseCase: GetThemeSongIdsUseCase,
    private val preferencesManager: PreferencesManager,
    private val themeSongController: ThemeSongController,
    private val okHttpClient: OkHttpClient,
    serverConnectionManager: ServerConnectionManager
) : BaseViewModel(serverConnectionManager) {

    private val cache = RepositoryCache()

    private var activeThemeSong: ThemeSongState? = themeSongController.currentThemeSong()
    private var playedItemIds: Set<String> = emptySet()
    private var preferredPlaybackItemId: String? = null

    sealed class DetailType {
        object Movie : DetailType()
        object Series : DetailType()
        object Season : DetailType()
        object Generic : DetailType()
    }

    private val _state = MutableStateFlow(MediaDetailState(themeSong = activeThemeSong))
    val state: StateFlow<MediaDetailState> = _state.asStateFlow()

    fun loadDetails(mediaId: String, userId: String, accessToken: String, type: DetailType = DetailType.Generic) {
        viewModelScope.launch {
            _state.value = MediaDetailState(isLoading = true, themeSong = activeThemeSong)
            try {
                val isOffline = serverConnectionManager.state.value.isOffline
                if (isOffline) {
                    val local = getMediaItemDetailLocalUseCase(
                        GetMediaItemDetailLocalUseCase.Params(userId, mediaId)
                    )
                    if (local != null) {
                        _state.value = _state.value.copy(
                            mediaItem = local,
                            playbackItem = defaultPlaybackItemFor(local),
                            isLoading = false,
                            error = null
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = AppError.Unknown("Media details unavailable offline")
                        )
                    }
                    return@launch
                }

                val detailResult = cache.get("detail_$mediaId") {
                    getMediaItemDetailUseCase(
                        GetMediaItemDetailUseCase.Params(userId, mediaId, accessToken)
                    )
                }
                when (detailResult) {
                    is NetworkResult.Success -> {
                        val mediaItem = detailResult.data
                        if (mediaItem.type.equals("episode", ignoreCase = true)) {
                            preferredPlaybackItemId = mediaItem.id
                        }
                        _state.value = _state.value.copy(
                            mediaItem = mediaItem,
                            playbackItem = defaultPlaybackItemFor(mediaItem),
                            isLoading = false,
                            error = null
                        )

                        
                        val actualType = if (type == DetailType.Generic) {
                            when (detailResult.data.type.lowercase()) {
                                "movie" -> DetailType.Movie
                                "series" -> DetailType.Series
                                "season" -> DetailType.Season
                                else -> DetailType.Generic
                            }
                        } else {
                            type
                        }

                        
                        launch { loadRelated(mediaId, userId, accessToken) }
                        launch { loadThemeSong(mediaItem, userId, accessToken) }

                        when (actualType) {
                            DetailType.Series -> {
                                launch { loadSeasons(mediaId, userId, accessToken) }
                            }
                            DetailType.Season -> {
                                launch { loadEpisodes(mediaId, userId, accessToken) }
                                mediaItem.seriesId?.let { seriesId ->
                                    launch { loadSeasons(seriesId, userId, accessToken) }
                                }
                            }
                            else -> {
                                if (mediaItem.type.equals("episode", ignoreCase = true)) {
                                    launch { loadEpisodesForEpisode(mediaItem, userId, accessToken) }
                                }
                            }
                        }
                    }
                    is NetworkResult.Error -> {
                        _state.value = _state.value.copy(isLoading = false, error = detailResult.error)
                    }
                    is NetworkResult.Loading -> Unit
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = AppError.Unknown("Failed to load media details: ${e.message}")
                )
            }
        }
    }

    private suspend fun loadRelated(mediaId: String, userId: String, accessToken: String) {
        when (val result = cache.get("related_$mediaId") {
            getRelatedResourcesBatchUseCase(
                GetRelatedResourcesBatchUseCase.Params(mediaId, userId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                _state.value = _state.value.copy(
                    similarItems = result.data.similar,
                    specialFeatures = result.data.special
                )
            }
            else -> { }
        }
    }

    private suspend fun loadSeasons(seriesId: String, userId: String, accessToken: String) {
        when (val result = cache.get("seasons_$seriesId") {
            getSeasonsUseCase(
                GetSeasonsUseCase.Params(userId, seriesId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                val sorted = result.data.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                _state.value = _state.value.copy(seasons = sorted)
                val episodesBySeason = mutableMapOf<String, List<MediaItem>>()
                val aggregatedEpisodes = mutableListOf<MediaItem>()
                for (season in sorted) {
                    val episodesResult = getEpisodesUseCase(
                        GetEpisodesUseCase.Params(userId, season.id, accessToken)
                    )
                    if (episodesResult is NetworkResult.Success) {
                        val sortedEpisodes = episodesResult.data
                            .sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                        val withPlayedState = applyPlayedStateToEpisodes(sortedEpisodes)
                        episodesBySeason[season.id] = withPlayedState
                        aggregatedEpisodes += withPlayedState
                    }
                }
                _state.value = _state.value.copy(episodesBySeasonId = episodesBySeason)
                if (aggregatedEpisodes.isNotEmpty()) {
                    updatePlaybackItemFromEpisodes(aggregatedEpisodes)
                }
            }
            else -> { }
        }
    }

    private suspend fun loadEpisodes(seasonId: String, userId: String, accessToken: String) {
        when (val result = cache.get("episodes_$seasonId") {
            getEpisodesUseCase(
                GetEpisodesUseCase.Params(userId, seasonId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                val sorted = result.data.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                val withPlayedState = applyPlayedStateToEpisodes(sorted)
                _state.value = _state.value.copy(episodes = withPlayedState)
                updatePlaybackItemFromEpisodes(withPlayedState)
            }
            else -> { }
        }
    }

    private suspend fun loadEpisodesForEpisode(
        mediaItem: MediaItem,
        userId: String,
        accessToken: String
    ) {
        val seasonId = mediaItem.seasonId
            ?: resolveSeasonId(mediaItem.seriesId, mediaItem.parentIndexNumber, userId, accessToken)
        seasonId?.let { loadEpisodes(it, userId, accessToken) }
    }

    private suspend fun resolveSeasonId(
        seriesId: String?,
        seasonNumber: Int?,
        userId: String,
        accessToken: String
    ): String? {
        if (seriesId.isNullOrBlank() || seasonNumber == null) return null
        val result = getSeasonsUseCase(GetSeasonsUseCase.Params(userId, seriesId, accessToken))
        if (result is NetworkResult.Success) {
            return result.data.firstOrNull { it.indexNumber == seasonNumber }?.id
        }
        return null
    }

    private fun defaultPlaybackItemFor(mediaItem: MediaItem): MediaItem? {
        return when (mediaItem.type.lowercase()) {
            "movie", "episode" -> mediaItem
            else -> null
        }
    }

    private fun updatePlaybackItemFromEpisodes(episodes: List<MediaItem>) {
        if (episodes.isEmpty()) {
            _state.value = _state.value.copy(playbackItem = null)
            return
        }

        val existingPlaybackId = _state.value.playbackItem?.id
        val preferredId = preferredPlaybackItemId ?: existingPlaybackId
        val preferredEpisode = preferredId?.let { id -> episodes.firstOrNull { it.id == id } }

        val episodeWithResumeProgress = episodes
            .filter { episode ->
                val playbackTicks = episode.userData?.playbackPositionTicks ?: 0L
                if (playbackTicks <= 0L) {
                    return@filter false
                }

                if (episode.userData?.played == true) {
                    return@filter false
                }

                val runtimeTicks = episode.runTimeTicks
                if (runtimeTicks == null) {
                    return@filter true
                }

                val completionThreshold = if (runtimeTicks > RESUME_COMPLETION_THRESHOLD) {
                    runtimeTicks - RESUME_COMPLETION_THRESHOLD
                } else {
                    runtimeTicks
                }
                playbackTicks < completionThreshold
            }
            .maxByOrNull { it.userData?.playbackPositionTicks ?: 0L }

        val firstUnplayedEpisode = episodes.firstOrNull { it.userData?.played != true }

        val selectedEpisode = episodeWithResumeProgress
            ?: firstUnplayedEpisode
            ?: episodes.first()

        val resolvedEpisode = preferredEpisode ?: selectedEpisode

        _state.value = _state.value.copy(playbackItem = resolvedEpisode)
        preferredPlaybackItemId = resolvedEpisode.id
    }

    private suspend fun loadPlaybackItemForSeason(
        seasonId: String,
        userId: String,
        accessToken: String
    ) {
        when (val result = cache.get("episodes_$seasonId") {
            getEpisodesUseCase(
                GetEpisodesUseCase.Params(userId, seasonId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                val sorted = result.data.sortedBy { it.indexNumber ?: Int.MAX_VALUE }
                val withPlayedState = applyPlayedStateToEpisodes(sorted)
                updatePlaybackItemFromEpisodes(withPlayedState)
            }
            else -> { }
        }
    }

    fun updatePlayedState(playedIds: Set<String>) {
        if (playedItemIds == playedIds) return
        playedItemIds = playedIds

        val currentState = _state.value
        val currentEpisodes = currentState.episodes
        val updatedEpisodes = currentEpisodes?.let { applyPlayedStateToEpisodes(it) }
        val episodesChanged = updatedEpisodes !== null && updatedEpisodes !== currentEpisodes
        val updatedMediaItem = applyPlayedStateToItem(currentState.mediaItem)
        val updatedPlaybackItem = applyPlayedStateToItem(currentState.playbackItem)

        if (episodesChanged || updatedMediaItem !== currentState.mediaItem ||
            updatedPlaybackItem !== currentState.playbackItem
        ) {
            _state.value = currentState.copy(
                episodes = updatedEpisodes,
                mediaItem = updatedMediaItem ?: currentState.mediaItem,
                playbackItem = updatedPlaybackItem ?: currentState.playbackItem
            )
            if (episodesChanged) {
                updatePlaybackItemFromEpisodes(updatedEpisodes)
            }
        }
    }

    private fun applyPlayedStateToEpisodes(episodes: List<MediaItem>): List<MediaItem> {
        if (episodes.isEmpty()) return episodes
        var changed = false
        val updatedEpisodes = episodes.map { episode ->
            val updatedEpisode = applyPlayedStateToItem(episode) ?: episode
            if (updatedEpisode !== episode) {
                changed = true
            }
            updatedEpisode
        }
        return if (changed) {
            updatedEpisodes
        } else {
            episodes
        }
    }

    private fun applyPlayedStateToItem(item: MediaItem?): MediaItem? {
        val current = item ?: return null
        val isEpisode = current.type.equals("episode", ignoreCase = true)
        if (!isEpisode) return current
        val isPlayed = playedItemIds.contains(current.id)
        val existingUserData = current.userData
        val newUserData = when {
            existingUserData == null && !isPlayed -> return current
            existingUserData == null && isPlayed -> UserData(played = true)
            existingUserData?.played == isPlayed -> return current
            else -> existingUserData?.copy(played = isPlayed)
        }
        return current.copy(userData = newUserData)
    }

    private suspend fun loadThemeSong(mediaItem: MediaItem, userId: String, accessToken: String) {
        val enabled = try { preferencesManager.getPlayThemeSongs().first() } catch (_: Exception) { false }
        if (!enabled) {
            if (activeThemeSong != null) {
                activeThemeSong = null
                themeSongController.clear()
                _state.value = _state.value.copy(themeSong = null)
            }
            return
        }

        val fallbackBase = runCatching { preferencesManager.getThemeSongFallbackUrl().first() }.getOrDefault("")
        val fallbackTvdbId = resolveFallbackTvdbId(mediaItem, userId, accessToken)
        val fallbackUrl = buildFallbackThemeSongUrl(fallbackTvdbId, fallbackBase)
        val fallbackId = fallbackTvdbId?.let { "fallback-$it" }

        var resolvedThemeSong: ThemeSongState? = null

        when (val idResult = cache.get("theme_ids_${mediaItem.id}") {
            getThemeSongIdsUseCase(
                GetThemeSongIdsUseCase.Params(mediaItem.id, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                val newId = idResult.data.firstOrNull()
                if (newId != null) {
                    val currentSong = activeThemeSong
                    if (currentSong != null && currentSong.id == newId) {
                        _state.value = _state.value.copy(themeSong = currentSong)
                        return
                    }

                    when (val themeResult = cache.get("themes_${mediaItem.id}") {
                        getThemeSongsUseCase(
                            GetThemeSongsUseCase.Params(mediaItem.id, accessToken)
                        )
                    }) {
                        is NetworkResult.Success -> {
                            val song = themeResult.data.firstOrNull()
                            val mediaSource = song?.mediaSources?.firstOrNull()
                            if (song != null && mediaSource != null) {
                                val serverUrl = preferencesManager.getServerUrl().first() ?: return
                                val container = mediaSource.container?.lowercase() ?: "mp3"
                                val url = buildString {
                                    append(serverUrl.removeSuffix("/"))
                                    append("/Audio/")
                                    append(song.id)
                                    append("/stream.")
                                    append(container)
                                    append("?static=true&mediaSourceId=")
                                    append(mediaSource.id)
                                    append("&api_key=")
                                    append(accessToken)
                                }
                                resolvedThemeSong = ThemeSongState(newId, url)
                            }
                        }
                        else -> Unit
                    }
                }
            }
            else -> Unit
        }

        if (resolvedThemeSong == null && fallbackUrl != null && fallbackId != null) {
            val canUseFallback = isFallbackAvailable(fallbackUrl)
            if (canUseFallback) {
                resolvedThemeSong = ThemeSongState(fallbackId, fallbackUrl)
            }
        }

        if (resolvedThemeSong != null) {
            activeThemeSong = resolvedThemeSong
            _state.value = _state.value.copy(themeSong = resolvedThemeSong)
        } else if (activeThemeSong != null) {
            activeThemeSong = null
            themeSongController.clear()
            _state.value = _state.value.copy(themeSong = null)
        }
    }

    private fun buildFallbackThemeSongUrl(tvdbId: String?, fallbackBaseUrl: String?): String? {
        val id = tvdbId?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val rawBase = fallbackBaseUrl?.trim()?.trimEnd('/')?.takeIf { it.isNotEmpty() } ?: return null
        val base = when {
            rawBase.startsWith("http://", ignoreCase = true) ||
                rawBase.startsWith("https://", ignoreCase = true) -> rawBase
            else -> "http://$rawBase"
        }
        return "$base/$id.mp3"
    }

    fun loadSeasonEpisodes(seasonId: String, userId: String, accessToken: String) {
        viewModelScope.launch {
            loadEpisodes(seasonId, userId, accessToken)
        }
    }

    private suspend fun resolveFallbackTvdbId(
        mediaItem: MediaItem,
        userId: String,
        accessToken: String
    ): String? {
        val type = mediaItem.type.lowercase()
        val isSeries = type == "series"
        val isSeasonOrEpisode = type == "season" || type == "episode"
        if (isSeries || !isSeasonOrEpisode) {
            mediaItem.tvdbId?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        }
        val seriesId = mediaItem.seriesId ?: return null
        val local = getMediaItemDetailLocalUseCase(GetMediaItemDetailLocalUseCase.Params(userId, seriesId))
        local?.tvdbId?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
        return when (
            val remote = getMediaItemDetailUseCase(
                GetMediaItemDetailUseCase.Params(userId, seriesId, accessToken)
            )
        ) {
            is NetworkResult.Success -> remote.data.tvdbId?.trim()?.takeIf { it.isNotEmpty() }
            else -> null
        }
    }

    private suspend fun isFallbackAvailable(url: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val headRequest = Request.Builder()
                .url(url)
                .head()
                .build()
            okHttpClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) return@withContext true
                if (response.code != 405) return@withContext false
            }

            val rangeRequest = Request.Builder()
                .url(url)
                .get()
                .addHeader("Range", "bytes=0-0")
                .build()
            okHttpClient.newCall(rangeRequest).execute().use { response ->
                response.isSuccessful
            }
        }.getOrElse { false }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refresh(mediaId: String, userId: String, accessToken: String, type: DetailType = DetailType.Generic) {
        loadDetails(mediaId, userId, accessToken, type)
    }

    fun clearState() {
        _state.value = MediaDetailState()
        preferredPlaybackItemId = null
    }

    fun setPreferredPlaybackItemId(itemId: String?) {
        preferredPlaybackItemId = itemId?.takeIf { it.isNotBlank() }
    }
}
