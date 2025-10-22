package com.hritwik.avoid.data.remote.dto.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LibraryResponse(
    @SerialName("Items")
    val items: List<BaseItemDto> = emptyList(),
    @SerialName("TotalRecordCount")
    val totalRecordCount: Int = 0
)