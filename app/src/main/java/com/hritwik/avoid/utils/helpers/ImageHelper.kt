package com.hritwik.avoid.utils.helpers

import com.hritwik.avoid.utils.constants.ApiConstants
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageHelper @Inject constructor() {
    fun createImageUrl(
        serverUrl: String,
        itemId: String,
        imageTag: String?,
        imageType: String = ApiConstants.IMAGE_TYPE_PRIMARY,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
        quality: Int = ApiConstants.DEFAULT_IMAGE_QUALITY
    ): String? {
        if (imageTag == null) return null

        val file = File(imageTag)
        if (file.isAbsolute) {
            return file.toURI().toString()
        }

        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        val params = mutableListOf<String>()

        params.add("tag=$imageTag")
        params.add("quality=$quality")
        maxWidth?.let { params.add("maxWidth=$it") }
        maxHeight?.let { params.add("maxHeight=$it") }

        return "${baseUrl}Items/$itemId/Images/$imageType?${params.joinToString("&")}" 
    }

    fun createPosterUrl(
        serverUrl: String,
        itemId: String,
        imageTag: String?,
    ): String? {
        return createImageUrl(
            serverUrl = serverUrl,
            itemId = itemId,
            imageTag = imageTag,
            imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
            maxWidth = ApiConstants.POSTER_MAX_WIDTH
        )
    }

    fun createBackdropUrl(
        serverUrl: String,
        itemId: String,
        imageTag: String?,
    ): String? {
        return createImageUrl(
            serverUrl = serverUrl,
            itemId = itemId,
            imageTag = imageTag,
            imageType = ApiConstants.IMAGE_TYPE_BACKDROP,
            maxWidth = ApiConstants.BACKDROP_MAX_WIDTH
        )
    }

    fun createThumbnailUrl(
        serverUrl: String,
        itemId: String,
        imageTag: String?,
    ): String? {
        return createImageUrl(
            serverUrl = serverUrl,
            itemId = itemId,
            imageTag = imageTag,
            imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
            maxWidth = ApiConstants.THUMBNAIL_MAX_WIDTH
        )
    }

    fun createLibraryImageUrl(
        serverUrl: String,
        libraryId: String,
        imageTag: String?,
    ): String? {
        return createImageUrl(
            serverUrl = serverUrl,
            itemId = libraryId,
            imageTag = imageTag,
            imageType = ApiConstants.IMAGE_TYPE_PRIMARY,
            maxWidth = ApiConstants.POSTER_MAX_WIDTH
        )
    }

    fun getBestImageTag(
        primaryImageTag: String?,
        backdropImageTags: List<String>
    ): String? {
        return primaryImageTag ?: backdropImageTags.firstOrNull()
    }

    fun shouldLoadImage(
        isWifiOnly: Boolean,
        isWifiConnected: Boolean,
        isMetered: Boolean
    ): Boolean {
        return if (isWifiOnly) {
            isWifiConnected
        } else {
            !isMetered || isWifiConnected
        }
    }
}
