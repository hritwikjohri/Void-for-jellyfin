package com.hritwik.avoid.presentation.ui.state

/**
 * Sections displayed on the home screen, ordered by loading priority.
 * Used by HomeContentLoadQueue to load content sequentially (FIFO).
 */
enum class HomeSection {
    CONTINUE_WATCHING,
    NEXT_UP,
    LATEST_MOVIES,
    LATEST_ITEMS,
    LATEST_EPISODES,
    PER_LIBRARY
}

enum class HomeSectionLoadState {
    QUEUED,
    LOADING,
    LOADED,
    ERROR
}

fun HomeSectionLoadState?.isLoadingOrQueued(): Boolean =
    this == HomeSectionLoadState.QUEUED || this == HomeSectionLoadState.LOADING
