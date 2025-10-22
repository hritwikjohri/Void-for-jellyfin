package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.model.library.MediaItem

data class SearchState(
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val suggestionsError: String? = null,
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val isJellyseerSearchEnabled: Boolean = false,
    val isJellyseerConfigured: Boolean = false,
    val jellyseerError: String? = null
)
