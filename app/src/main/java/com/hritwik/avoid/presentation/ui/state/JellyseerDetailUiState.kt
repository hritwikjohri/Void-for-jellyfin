package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.model.jellyseer.JellyseerMediaDetail
import com.hritwik.avoid.domain.model.jellyseer.JellyseerSearchResult
import com.hritwik.avoid.domain.model.jellyseer.JellyseerVideoQuality

data class JellyseerDetailUiState(
    val isLoading: Boolean = false,
    val detail: JellyseerMediaDetail? = null,
    val error: String? = null,
    val isRequesting: Boolean = false,
    val requestMessage: String? = null,
    val requestError: String? = null,
    val availableVideoQualities: List<JellyseerVideoQuality> = JellyseerVideoQuality.entries.toList(),
    val disabledVideoQualities: Set<JellyseerVideoQuality> = emptySet(),
    val selectedVideoQuality: JellyseerVideoQuality = JellyseerVideoQuality.default,
    val exactTitleOptions: List<JellyseerSearchResult> = emptyList(),
    val requireExactTitleSelection: Boolean = false
)
