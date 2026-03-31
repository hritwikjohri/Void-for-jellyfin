package com.hritwik.avoid.presentation.ui.state

import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.helpers.AlphaScrollHelper

data class LibraryItemsState(
    val isLoading: Boolean = false,
    val items: List<MediaItem> = emptyList(),
    val allTitles: List<String> = emptyList(),
    val hasMorePages: Boolean = false,
    val currentPage: Int = 0,
    val error: String? = null,
    val sectionedItems: List<AlphaScrollHelper.LibraryGridItem> = emptyList(),
    val sectionHeaderIndices: Map<Char, Int> = emptyMap()
)