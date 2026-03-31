package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.library.MediaItem

data class PersonDetailState(
    val isLoading: Boolean = false,
    val error: AppError? = null,
    val personDetail: MediaItem? = null,
    val appearances: List<MediaItem> = emptyList(),
    val isLoadingAppearances: Boolean = false
)
