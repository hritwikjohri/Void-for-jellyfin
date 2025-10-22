package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.library.MediaItem

data class MediaDetailState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val mediaItem: MediaItem? = null,
    val playbackItem: MediaItem? = null,
    val similarItems: List<MediaItem> = emptyList(),
    val specialFeatures: List<MediaItem> = emptyList(),
    val seasons: List<MediaItem>? = null,
    val episodes: List<MediaItem>? = null,
    val themeSong: ThemeSongState? = null
)
