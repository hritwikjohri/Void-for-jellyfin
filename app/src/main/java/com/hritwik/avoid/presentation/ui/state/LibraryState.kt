package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.model.library.Library
import com.hritwik.avoid.domain.model.library.MediaItem

data class LibraryState(
    val isLoading: Boolean = false,
    val libraries: List<Library> = emptyList(),

    
    val latestItems: List<MediaItem> = emptyList(),
    val resumeItems: List<MediaItem> = emptyList(),
    val nextUpItems: List<MediaItem> = emptyList(),

    
    val latestEpisodes: List<MediaItem> = emptyList(),
    val latestMovies: List<MediaItem> = emptyList(),
    val recentlyReleasedMovies: List<MediaItem> = emptyList(),
    val recentlyReleasedShows: List<MediaItem> = emptyList(),

    
    val recommendedItems: List<MediaItem> = emptyList(),
    val trendingItems: List<MediaItem> = emptyList(),
    val favoriteItems: List<MediaItem> = emptyList(),
    val watchlistItems: List<MediaItem> = emptyList(),
    val collections: List<MediaItem> = emptyList(),
    val collectionPreviews: Map<String, List<MediaItem>> = emptyMap(),

    
    val error: String? = null
)