package com.hritwik.avoid.presentation.viewmodel.library

import com.hritwik.avoid.presentation.ui.state.HomeSection
import com.hritwik.avoid.presentation.ui.state.HomeSectionLoadState

/**
 * FIFO queue for loading home screen sections sequentially.
 * Sections are enqueued in priority order and processed one at a time,
 * ensuring the most important content (Continue Watching) appears first
 * before loading subsequent sections.
 *
 * @param onStateChanged callback invoked when a section's load state changes,
 *        allowing the ViewModel to update LibraryState.sectionStates.
 */
class HomeContentLoadQueue(
    private val onStateChanged: (HomeSection, HomeSectionLoadState) -> Unit
) {
    private val tasks = ArrayDeque<Pair<HomeSection, suspend () -> Unit>>()

    fun enqueue(section: HomeSection, loader: suspend () -> Unit) {
        tasks.addLast(section to loader)
        onStateChanged(section, HomeSectionLoadState.QUEUED)
    }

    suspend fun processAll() {
        while (tasks.isNotEmpty()) {
            val (section, loader) = tasks.removeFirst()
            onStateChanged(section, HomeSectionLoadState.LOADING)
            try {
                loader()
                onStateChanged(section, HomeSectionLoadState.LOADED)
            } catch (e: Exception) {
                onStateChanged(section, HomeSectionLoadState.ERROR)
            }
        }
    }
}
