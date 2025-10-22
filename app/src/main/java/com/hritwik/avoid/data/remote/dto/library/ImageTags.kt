package com.hritwik.avoid.data.remote.dto.library

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ImageTags(
    @SerialName("Primary")
    val primary: String? = null,
    @SerialName("Banner")
    val banner: String? = null,
    @SerialName("Thumb")
    val thumb: String? = null,
    @SerialName("Logo")
    val logo: String? = null
)