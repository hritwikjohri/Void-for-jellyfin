package com.hritwik.avoid.domain.model.library

data class Library(
    val id: String,
    val name: String,
    val type: LibraryType,
    val itemCount: Int? = null,
    val primaryImageTag: String? = null,
    val isFolder: Boolean = true
)