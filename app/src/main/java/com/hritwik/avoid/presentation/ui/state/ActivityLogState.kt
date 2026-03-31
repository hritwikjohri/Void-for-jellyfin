package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.error.AppError
import com.hritwik.avoid.domain.model.activity.ActivityLogEntry

data class ActivityLogState(
    val activities: List<ActivityLogEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: AppError? = null,
    val hasMore: Boolean = true,
    val currentPage: Int = 0,
    val totalRecordCount: Int = 0
)
