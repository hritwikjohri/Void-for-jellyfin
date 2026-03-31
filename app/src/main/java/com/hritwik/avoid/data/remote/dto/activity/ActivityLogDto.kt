package com.hritwik.avoid.data.remote.dto.activity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityLogEntryDto(
    @SerialName("Id")
    val id: Long,
    @SerialName("Name")
    val name: String,
    @SerialName("Overview")
    val overview: String? = null,
    @SerialName("ShortOverview")
    val shortOverview: String? = null,
    @SerialName("Type")
    val type: String,
    @SerialName("ItemId")
    val itemId: String? = null,
    @SerialName("Date")
    val date: String,
    @SerialName("UserId")
    val userId: String? = null,
    @SerialName("UserPrimaryImageTag")
    val userPrimaryImageTag: String? = null,
    @SerialName("Severity")
    val severity: String? = null
)

@Serializable
data class ActivityLogEntryQueryResult(
    @SerialName("Items")
    val items: List<ActivityLogEntryDto> = emptyList(),
    @SerialName("TotalRecordCount")
    val totalRecordCount: Int = 0,
    @SerialName("StartIndex")
    val startIndex: Int = 0
)
