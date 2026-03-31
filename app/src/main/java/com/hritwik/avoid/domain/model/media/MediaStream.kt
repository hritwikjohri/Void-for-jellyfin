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
    val profile: String? = null,
    val language: String?,
    val displayLanguage: String?,
    val title: String?,
    val displayTitle: String?,
    val videoRange: String? = null,
    val videoRangeType: String? = null,
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

    val dynamicRangeLabel: String
        get() {
            val normalizedType = videoRangeType
                ?.replace("_", "")
                ?.replace("-", "")
                ?.uppercase()

            when (normalizedType) {
                "SDR" -> return "SDR"
                "HDR10" -> return "HDR10"
                "HDR10PLUS" -> return "HDR10+"
                "HLG" -> return "HLG"
                "DOVI" -> return "DoVi"
                "DOVIWITHHDR10" -> return "DoVi/HDR10"
                "DOVIWITHHLG" -> return "DoVi/HLG"
                "DOVIWITHSDR" -> return "DoVi/SDR"
                "DOVIWITHEL" -> return "DoVi EL"
                "DOVIWITHHDR10PLUS" -> return "DoVi/HDR10+"
                "DOVIWITHELHDR10PLUS" -> return "DoVi EL/HDR10+"
                "DOVIINVALID" -> return "DoVi"
            }

            return when (videoRange?.uppercase()) {
                "HDR" -> "HDR"
                "SDR" -> "SDR"
                else -> "SDR"
            }
        }

    
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
