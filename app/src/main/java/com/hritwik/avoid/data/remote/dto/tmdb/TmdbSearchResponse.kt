package com.hritwik.avoid.data.remote.dto.tmdb

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbSearchResponse(
    @SerialName("results")
    val results: List<TmdbSearchResult> = emptyList()
)

@Serializable
data class TmdbSearchResult(
    @SerialName("media_type")
    val mediaType: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("original_name")
    val originalName: String? = null
)

@Serializable
data class TmdbTitleResponse(
    @SerialName("results")
    val results: List<TmdbTitleResult> = emptyList()
)

@Serializable
data class TmdbTitleResult(
    @SerialName("title")
    val title: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("original_name")
    val originalName: String? = null
)
