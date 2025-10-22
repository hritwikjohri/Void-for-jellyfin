package com.hritwik.avoid.domain.model.jellyseer

data class JellyseerRequest(
    val id: Long,
    val status: JellyseerRequestStatus,
    val is4k: Boolean,
    val requestedBy: String?
)

data class JellyseerMediaInfo(
    val status: JellyseerAvailabilityStatus,
    val requests: List<JellyseerRequest>,
    val tmdbId: Long? = null,
    val tvdbId: Long? = null
) {
    val hasPendingRequest: Boolean
        get() = requests.any { it.status.isActiveRequest }

    val hasApprovedRequest: Boolean
        get() = requests.any {
            it.status == JellyseerRequestStatus.APPROVED ||
                it.status == JellyseerRequestStatus.COMPLETED
        }

    val activeRequestQualities: Set<JellyseerVideoQuality>
        get() = requests
            .filter { it.status.isActiveRequest }
            .mapTo(mutableSetOf()) { it.toQuality() }
}

data class JellyseerSearchResult(
    val id: Long,
    val mediaType: JellyseerMediaType,
    val title: String,
    val overview: String?,
    val releaseDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Double?,
    val mediaInfo: JellyseerMediaInfo?
)

data class JellyseerMediaDetail(
    val id: Long,
    val mediaType: JellyseerMediaType,
    val title: String,
    val overview: String?,
    val tagline: String?,
    val releaseDate: String?,
    val runtimeMinutes: Int?,
    val posterPath: String?,
    val backdropPath: String?,
    val genres: List<String>,
    val voteAverage: Double?,
    val mediaInfo: JellyseerMediaInfo?,
    val episodeRunTime: Int?,
    val numberOfSeasons: Int?,
    val numberOfEpisodes: Int?,
    val cast: List<JellyseerCastMember>
) {
    val isRequestable: Boolean
        get() {
            val info = mediaInfo ?: return true
            val activeQualities = info.activeRequestQualities
            return JellyseerVideoQuality.entries.any { it !in activeQualities }
        }

    val availability: JellyseerAvailabilityStatus
        get() = mediaInfo?.status ?: JellyseerAvailabilityStatus.UNKNOWN
}

data class JellyseerCastMember(
    val name: String?,
    val character: String?,
    val profilePath: String?
)

data class JellyseerPagedResult<T>(
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<T>
)

private fun JellyseerRequest.toQuality(): JellyseerVideoQuality =
    if (is4k) JellyseerVideoQuality.UHD else JellyseerVideoQuality.HD
