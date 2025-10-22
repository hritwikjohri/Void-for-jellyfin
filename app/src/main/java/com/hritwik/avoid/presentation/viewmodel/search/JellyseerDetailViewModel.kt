package com.hritwik.avoid.presentation.viewmodel.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hritwik.avoid.data.common.NetworkResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaDetail
import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaType
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerVideoQuality
import com.hritwik.avoid.domain.usecase.jellyseer.CreateJellyseerRequestUseCase
import com.hritwik.avoid.domain.usecase.jellyseer.GetJellyseerMediaDetailsUseCase
import com.hritwik.avoid.domain.usecase.jellyseer.SearchJellyseerMediaUseCase
import com.hritwik.avoid.presentation.ui.state.JellyseerDetailUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JellyseerDetailViewModel @Inject constructor(
    private val getDetailsUseCase: GetJellyseerMediaDetailsUseCase,
    private val createRequestUseCase: CreateJellyseerRequestUseCase,
    private val searchUseCase: SearchJellyseerMediaUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(JellyseerDetailUiState(isLoading = true))
    val state: StateFlow<JellyseerDetailUiState> = _state.asStateFlow()

    private var currentLoadJob: Job? = null

    fun loadDetails(mediaId: Long, mediaType: JellyseerMediaType) {
        currentLoadJob?.cancel()
        currentLoadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getDetailsUseCase(
                GetJellyseerMediaDetailsUseCase.Params(
                    id = mediaId,
                    type = mediaType
                )
            )) {
                is NetworkResult.Success -> {
                    val detail = result.data
                    val disabledQualities = detail.disabledQualities()
                    val availableQualities = JellyseerVideoQuality.entries.toList()
                    val selectedQuality = stateValue().selectedVideoQuality
                        .takeUnless { disabledQualities.contains(it) }
                        ?: availableQualities.firstOrNull { it !in disabledQualities }
                        ?: JellyseerVideoQuality.default

                    val exactMatches = detail.findExactTitleMatches()

                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            detail = detail,
                            error = null,
                            availableVideoQualities = availableQualities,
                            disabledVideoQualities = disabledQualities,
                            selectedVideoQuality = selectedQuality,
                            exactTitleOptions = exactMatches,
                            requireExactTitleSelection = exactMatches.size > 1,
                            requestMessage = null,
                            requestError = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _state.update { state ->
                        state.copy(
                            isLoading = false,
                            error = result.message,
                            detail = null
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    _state.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun requestMedia(mediaId: Long, mediaType: JellyseerMediaType) {
        val currentDetail = _state.value.detail ?: return
        if (_state.value.isRequesting) return

        viewModelScope.launch {
            val selectedQuality = _state.value.selectedVideoQuality
            _state.update { it.copy(isRequesting = true, requestMessage = null, requestError = null) }
            val result = createRequestUseCase(
                CreateJellyseerRequestUseCase.Params(
                    id = currentDetail.id,
                    type = currentDetail.mediaType,
                    is4k = selectedQuality.is4k,
                    tvdbId = currentDetail.mediaInfo?.tvdbId
                )
            )
            when (result) {
                is NetworkResult.Success -> {
                    _state.update {
                        it.copy(
                            isRequesting = false,
                            requestMessage = "${selectedQuality.displayName} request submitted successfully"
                        )
                    }
                    loadDetails(currentDetail.id, currentDetail.mediaType)
                }
                is NetworkResult.Error -> {
                    _state.update { it.copy(isRequesting = false, requestError = result.message) }
                }
                is NetworkResult.Loading -> {
                    _state.update { it.copy(isRequesting = true) }
                }
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(requestMessage = null, requestError = null) }
    }

    fun selectVideoQuality(quality: JellyseerVideoQuality) {
        if (_state.value.disabledVideoQualities.contains(quality)) return
        _state.update { it.copy(selectedVideoQuality = quality) }
    }

    fun selectExactTitle(result: JellyseerSearchResult) {
        val current = _state.value.detail
        if (current?.id == result.id && current.mediaType == result.mediaType) {
            return
        }
        loadDetails(result.id, result.mediaType)
    }

    private fun stateValue(): JellyseerDetailUiState = _state.value

    private suspend fun JellyseerMediaDetail.findExactTitleMatches(): List<JellyseerSearchResult> {
        if (title.isBlank()) return emptyList()
        val normalizedTitle = title.normalizeTitle()
        val searchResult = searchUseCase(
            SearchJellyseerMediaUseCase.Params(
                query = title,
                page = 1
            )
        )

        val matches = when (searchResult) {
            is NetworkResult.Success -> {
                searchResult.data.results.filter { result ->
                    result.title.normalizeTitle() == normalizedTitle
                }
            }
            else -> emptyList()
        }

        val combined = if (matches.any { it.id == id }) {
            matches
        } else {
            matches + JellyseerSearchResult(
                id = id,
                mediaType = mediaType,
                title = title,
                overview = overview,
                releaseDate = releaseDate,
                posterPath = posterPath,
                backdropPath = backdropPath,
                voteAverage = voteAverage,
                mediaInfo = mediaInfo
            )
        }

        return combined.distinctBy { it.id }
    }

    private fun JellyseerMediaDetail.disabledQualities(): Set<JellyseerVideoQuality> =
        mediaInfo?.activeRequestQualities ?: emptySet()

    private fun String.normalizeTitle(): String = lowercase()
        .replace("&", "and")
        .replace(Regex("[^a-z0-9]"), "")
}