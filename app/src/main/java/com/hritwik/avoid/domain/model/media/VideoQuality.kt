package com.hritwik.avoid.domain.model.media

import kotlinx.serialization.Serializable

@Serializable
enum class VideoQuality(
    val displayName: String,
    val minWidth: Int,
    val minHeight: Int,
) {
    UHD_4K("4K UHD", minWidth = 3840, minHeight = 2160),
    QHD_2K("2K QHD", minWidth = 2560, minHeight = 1440),
    FHD_1080P("1080p FHD", minWidth = 1920, minHeight = 1080),
    HD_720P("720p HD", minWidth = 1280, minHeight = 720),
    SD_480P("480p SD", minWidth = 0, minHeight = 480);

    val width: Int
        get() = minWidth

    val height: Int
        get() = minHeight

    companion object {
        fun fromDimensions(width: Int?, height: Int?): VideoQuality {
            width?.let { horizontalPixels ->
                return when {
                    horizontalPixels >= UHD_4K.minWidth -> UHD_4K
                    horizontalPixels >= QHD_2K.minWidth -> QHD_2K
                    horizontalPixels >= FHD_1080P.minWidth -> FHD_1080P
                    horizontalPixels >= HD_720P.minWidth -> HD_720P
                    else -> SD_480P
                }
            }

            val verticalPixels = height ?: return SD_480P
            return when {
                verticalPixels >= UHD_4K.minHeight -> UHD_4K
                verticalPixels >= QHD_2K.minHeight -> QHD_2K
                verticalPixels >= FHD_1080P.minHeight -> FHD_1080P
                verticalPixels >= HD_720P.minHeight -> HD_720P
                else -> SD_480P
            }
        }
    }
}
