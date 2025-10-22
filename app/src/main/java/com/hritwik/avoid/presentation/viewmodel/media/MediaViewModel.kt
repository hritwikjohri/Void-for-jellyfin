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
import com.hritwik.avoid.utils.helpers.ConnectivityObserver
import com.hritwik.avoid.presentation.viewmodel.BaseViewModel
import com.hritwik.avoid.utils.constants.AppConstants.RESUME_COMPLETION_THRESHOLD
import com.hritwik.avoid.domain.model.library.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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
    connectivityObserver: ConnectivityObserver
) : BaseViewModel(connectivityObserver) {

    private val cache = RepositoryCache()

    private var activeThemeSong: ThemeSongState? = themeSongController.currentThemeSong()
    private var playedItemIds: Set<String> = emptySet()

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
                val isOffline = !isConnected.value
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
                        launch { loadThemeSong(mediaId, accessToken) }

                        when (actualType) {
                            DetailType.Series -> {
                                launch { loadSeasons(mediaId, userId, accessToken) }
                            }
                            DetailType.Season -> {
                                launch { loadEpisodes(mediaId, userId, accessToken) }
                            }
                            else -> {}
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
                sorted.firstOrNull()?.let { season ->
                    loadPlaybackItemForSeason(season.id, userId, accessToken)
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

        _state.value = _state.value.copy(playbackItem = selectedEpisode)
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

    private suspend fun loadThemeSong(mediaId: String, accessToken: String) {
        val enabled = try { preferencesManager.getPlayThemeSongs().first() } catch (_: Exception) { false }
        if (!enabled) {
            if (activeThemeSong != null) {
                activeThemeSong = null
                themeSongController.clear()
                _state.value = _state.value.copy(themeSong = null)
            }
            return
        }

        when (val idResult = cache.get("theme_ids_$mediaId") {
            getThemeSongIdsUseCase(
                GetThemeSongIdsUseCase.Params(mediaId, accessToken)
            )
        }) {
            is NetworkResult.Success -> {
                val newId = idResult.data.firstOrNull()
                if (newId == null) {
                    if (activeThemeSong != null) {
                        activeThemeSong = null
                        themeSongController.clear()
                        _state.value = _state.value.copy(themeSong = null)
                    }
                    return
                }

                val currentSong = activeThemeSong
                if (currentSong != null && currentSong.id == newId) {
                    _state.value = _state.value.copy(themeSong = currentSong)
                    return
                }

                when (val themeResult = cache.get("themes_$mediaId") {
                    getThemeSongsUseCase(
                        GetThemeSongsUseCase.Params(mediaId, accessToken)
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
                            val themeSongState = ThemeSongState(newId, url)
                            activeThemeSong = themeSongState
                            _state.value = _state.value.copy(themeSong = themeSongState)
                        } else {
                            activeThemeSong = null
                            themeSongController.clear()
                            _state.value = _state.value.copy(themeSong = null)
                        }
                    }
                    else -> Unit
                }
            }
            else -> Unit
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun refresh(mediaId: String, userId: String, accessToken: String, type: DetailType = DetailType.Generic) {
        loadDetails(mediaId, userId, accessToken, type)
    }

    fun clearState() {
        _state.value = MediaDetailState()
    }
}