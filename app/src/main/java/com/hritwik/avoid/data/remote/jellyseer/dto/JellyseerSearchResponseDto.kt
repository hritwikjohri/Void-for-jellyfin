package com.hritwik.avoid.data.remote.jellyseer.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class JellyseerSearchResponseDto(
    val page: Int = 1,
    @SerialName("totalPages") val totalPages: Int = 0,
    @SerialName("totalResults") val totalResults: Int = 0,
    val results: List<JellyseerSearchItemDto> = emptyList()
)

@Serializable
data class JellyseerSearchItemDto(
    val id: Long,
    @SerialName("mediaType") val mediaType: String,
    val title: String? = null,
    val name: String? = null,
    val overview: String? = null,
    @SerialName("posterPath") val posterPath: String? = null,
    @SerialName("backdropPath") val backdropPath: String? = null,
    @SerialName("releaseDate") val releaseDate: String? = null,
    @SerialName("firstAirDate") val firstAirDate: String? = null,
    @SerialName("voteAverage") val voteAverage: Double? = null,
    @SerialName("voteCount") val voteCount: Int? = null,
    @SerialName("mediaInfo") val mediaInfo: JellyseerMediaInfoDto? = null
)
