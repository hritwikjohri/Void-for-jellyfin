package com.hritwik.avoid.data.remote.dto.playback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SegmentResponse(
    @SerialName("Items") val items: List<SegmentDto>,
    @SerialName("TotalRecordCount") val totalRecordCount: Int? = null
)