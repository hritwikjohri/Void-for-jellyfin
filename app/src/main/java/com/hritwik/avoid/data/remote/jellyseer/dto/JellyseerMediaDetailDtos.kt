package com.hritwik.avoid.data.remote.jellyseer.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JellyseerMovieDetailDto(
    val id: Long,
    val title: String? = null,
    val overview: String? = null,
    val runtime: Int? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    val genres: List<JellyseerGenreDto> = emptyList(),
    @SerialName("voteAverage") val voteAverage: Double? = null,
    val tagline: String? = null,
    @SerialName("mediaInfo") val mediaInfo: JellyseerMediaInfoDto? = null,
    val credits: JellyseerCreditsDto? = null
)

@Serializable
data class JellyseerTvDetailDto(
    val id: Long,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("lastAirDate") val lastAirDate: String? = null,
    @SerialName("episodeRunTime") val episodeRunTime: List<Int>? = null,
    @SerialName("numberOfSeasons") val numberOfSeasons: Int? = null,
    @SerialName("numberOfEpisodes") val numberOfEpisodes: Int? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    val genres: List<JellyseerGenreDto> = emptyList(),
    @SerialName("voteAverage") val voteAverage: Double? = null,
    val tagline: String? = null,
    @SerialName("mediaInfo") val mediaInfo: JellyseerMediaInfoDto? = null,
    val credits: JellyseerCreditsDto? = null
)

@Serializable
data class JellyseerGenreDto(
    val id: Long,
    val name: String
)

@Serializable
data class JellyseerCreditsDto(
    val cast: List<JellyseerCastDto> = emptyList()
)

@Serializable
data class JellyseerCastDto(
    val id: Long? = null,
    val name: String? = null,
    val character: String? = null,
    @SerialName("profilePath") val profilePath: String? = null
)
