package com.hritwik.avoid.data.remote.dto.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PersonDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String? = null,
    @SerialName("Role")
    val role: String? = null,
    @SerialName("Type")
    val type: String? = null,
    @SerialName("PrimaryImageTag")
    val primaryImageTag: String? = null
)
