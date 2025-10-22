package com.hritwik.avoid.presentation.ui.state

data class LibraryGenresState(
    val isLoading: Boolean = false,
    val genres: List<String> = emptyList(),
    val error: String? = null
)
