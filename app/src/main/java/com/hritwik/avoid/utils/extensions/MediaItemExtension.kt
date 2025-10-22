package com.hritwik.avoid.utils.extensions

import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.utils.constants.ApiConstants
import java.io.File



fun MediaItem.getBackdropUrl(
    serverUrl: String,
    quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY,
    maxWidth: Int = ApiConstants.BACKDROP_MAX_WIDTH
): String {
    val imageTag = backdropImageTags.firstOrNull()
    imageTag?.let {
        val file = File(it)
        if (file.isAbsolute) return file.toURI().toString()
    }
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$id/Images/Backdrop?tag=$imageTag&quality=$quality&maxWidth=$maxWidth"
}

fun MediaItem.getPosterUrl(
    serverUrl: String,
    quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY,
    maxWidth: Int = ApiConstants.POSTER_MAX_WIDTH
): String {
    val imageTag = primaryImageTag ?: return ""
    val file = File(imageTag)
    if (file.isAbsolute) return file.toURI().toString()
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$id/Images/Primary?tag=$imageTag&quality=$quality&maxWidth=$maxWidth"
}

fun MediaItem.getLogoUrl(
    serverUrl: String,
    quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY,
    maxWidth: Int = ApiConstants.POSTER_MAX_WIDTH
): String? {
    val imageTag = this.logoImageTag ?: return null
    val file = File(imageTag)
    if (file.isAbsolute) return file.toURI().toString()
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$id/Images/Logo?tag=$imageTag&quality=$quality&maxWidth=$maxWidth"
}

fun MediaItem.getImageUrl(
    serverUrl: String,
    imageType: String,
    imageTag: String?,
    quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY,
    maxWidth: Int = ApiConstants.BACKDROP_MAX_WIDTH
): String? {
    if (imageTag == null) return null
    val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
    return "${baseUrl}Items/$id/Images/$imageType?tag=$imageTag&quality=$quality&maxWidth=$maxWidth"
}


fun List<MediaItem>.getNextUnwatchedEpisode(): MediaItem? {
    return find { it.userData?.played != true }
}


fun List<MediaItem>.getWatchedCount(): Int {
    return count { it.userData?.played == true }
}