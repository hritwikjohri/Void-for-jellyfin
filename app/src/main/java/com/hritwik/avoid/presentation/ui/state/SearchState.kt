package com.hritwik.avoid.presentation.ui.state

enum class SearchCategory {
    TopResults,
    Movies,
    Shows,
    Episodes
}

data class SearchState(
    val searchQuery: String = "",
    val recentSearches: List<String> = emptyList(),
    val error: String? = null,
    val isSearchActive: Boolean = false,
    val selectedCategory: SearchCategory = SearchCategory.TopResults,
    val isJellyseerSearchEnabled: Boolean = false,
    val isJellyseerConfigured: Boolean = false,
    val jellyseerError: String? = null
)
