package com.hritwik.avoid.domain.model.library


data class HomeScreenData(
    val libraries: List<Library>,
    val latestItems: List<MediaItem>,
    val resumeItems: List<MediaItem>,
    val nextUpItems: List<MediaItem>,
    val latestEpisodes: List<MediaItem>,
    val latestMovies: List<MediaItem>,
    val movies: List<MediaItem>,
    val shows: List<MediaItem>,
    val recentlyReleasedMovies: List<MediaItem>,
    val recentlyReleasedShows: List<MediaItem>,
    val recommendedItems: List<MediaItem>,
    val collections: List<MediaItem>,
    val errors: List<String> = emptyList()
)
