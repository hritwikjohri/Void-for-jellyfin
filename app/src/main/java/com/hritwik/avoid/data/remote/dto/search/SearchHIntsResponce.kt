package com.hritwik.avoid.data.remote.dto.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchHintsResponse(
    @SerialName("SearchHints")
    val searchHints: List<SearchHintDto> = emptyList()
)

@Serializable
data class SearchHintDto(
    @SerialName("Name")
    val name: String? = null
)