package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.model.library.MediaItem

data class FavoritesState(
    val isLoading: Boolean = false,
    val favorites: List<MediaItem> = emptyList(),
    val error: String? = null
)