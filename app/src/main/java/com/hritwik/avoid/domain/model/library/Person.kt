package com.hritwik.avoid.domain.model.library

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import com.hritwik.avoid.utils.constants.ApiConstants
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class Person(
    val id: String,
    val name: String,
    val role: String? = null,
    val type: String? = null,
    val primaryImageTag: String? = null
) : Parcelable {
    fun getImageUrl(serverUrl: String): String? {
        if (primaryImageTag == null) return null
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return "${baseUrl}Items/$id/Images/Primary?tag=$primaryImageTag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.POSTER_MAX_WIDTH}"
    }
}
