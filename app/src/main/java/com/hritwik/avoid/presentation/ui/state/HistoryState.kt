package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.model.library.MediaItem


data class HistoryState(
    val isLoading: Boolean = false,
    val history: List<MediaItem> = emptyList(),
    val error: String? = null
)

