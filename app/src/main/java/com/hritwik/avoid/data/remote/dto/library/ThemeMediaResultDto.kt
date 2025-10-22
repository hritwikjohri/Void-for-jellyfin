package com.hritwik.avoid.data.remote.dto.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ThemeMediaResultDto(
    @SerialName("Items")
    val items: List<BaseItemDto> = emptyList()
)

@Serializable
data class AllThemeMediaResultDto(
    @SerialName("ThemeSongsResult")
    val themeSongsResult: ThemeMediaResultDto? = null
)
