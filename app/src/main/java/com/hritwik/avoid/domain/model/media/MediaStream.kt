package com.hritwik.avoid.domain.model.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import org.jellyfin.sdk.model.api.MediaStreamType

@Serializable
@Parcelize
data class MediaStream(
    val index: Int,
    val type: MediaStreamType,
    val codec: String?,
    val language: String?,
    val displayLanguage: String?,
    val title: String?,
    val displayTitle: String?,
    val isDefault: Boolean,
    val isForced: Boolean,
    val isExternal: Boolean,
    val bitRate: Int?,
    val width: Int?,
    val height: Int?,
    val aspectRatio: String?,
    val frameRate: Float?,
    val channels: Int?,
    val sampleRate: Int?,
    val channelLayout: String?
) : Parcelable {
    
    val resolution: String?
        get() = if (width != null && height != null) "${width}x${height}" else null

    val videoQuality: VideoQuality?
        get() = VideoQuality.fromDimensions(width, height)

    
    val audioDescription: String
        get() = buildString {
            displayTitle?.let { append(it) } ?: run {
                codec?.let { append(it.uppercase()) }
                channels?.let {
                    if (isNotEmpty()) append(" ")
                    append("${it}ch")
                }
                displayLanguage?.let {
                    if (isNotEmpty()) append(" ")
                    append("($it)")
                }
            }
        }.ifEmpty { "Unknown Audio" }

    
    val subtitleDescription: String
        get() = buildString {
            displayTitle?.let { append(it) } ?: run {
                displayLanguage?.let { append(it) }
                if (isForced) {
                    if (isNotEmpty()) append(" ")
                    append("(Forced)")
                }
                if (isExternal) {
                    if (isNotEmpty()) append(" ")
                    append("(External)")
                }
            }
        }.ifEmpty { "Unknown Subtitle" }
}