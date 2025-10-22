package com.hritwik.avoid.domain.model.jellyseer

enum class JellyseerMediaType(val value: String) {
    MOVIE("movie"),
    TV("tv");

    companion object {
        fun from(raw: String?): JellyseerMediaType? = when (raw?.lowercase()) {
            MOVIE.value -> MOVIE
            TV.value -> TV
            else -> null
        }
    }
}
