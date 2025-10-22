package com.hritwik.avoid.domain.model.library

enum class LibraryType {
    MOVIES,
    TV_SHOWS,
    MUSIC,
    BOOKS,
    PHOTOS,
    MIXED,
    COLLECTIONS,
    UNKNOWN;

    companion object {
        fun fromString(type: String?): LibraryType {
            return when (type?.lowercase()) {
                "movies" -> MOVIES
                "tvshows" -> TV_SHOWS
                "music" -> MUSIC
                "books" -> BOOKS
                "photos" -> PHOTOS
                "mixed" -> MIXED
                "boxsets", "collections" -> COLLECTIONS
                else -> UNKNOWN
            }
        }
    }
}