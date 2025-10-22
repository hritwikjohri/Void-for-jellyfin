package com.hritwik.avoid.presentation.ui.state

data class SearchFilters(
    val includeMovies: Boolean = true,
    val includeTvShows: Boolean = true,
    val includeEpisodes: Boolean = true,
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val minRating: Double? = null
) {
    fun toItemTypes(): List<String> {
        val types = mutableListOf<String>()
        if (includeMovies) types.add("Movie")
        if (includeTvShows) types.add("Series")
        if (includeEpisodes) types.add("Episode")
        return types
    }
}