package com.hritwik.avoid.data.remote.dto.media

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaStreamDto(
    @SerialName("Index")
    val index: Int,
    @SerialName("Type")
    val type: String,
    @SerialName("Codec")
    val codec: String? = null,
    @SerialName("Language")
    val language: String? = null,
    @SerialName("DisplayLanguage")
    val displayLanguage: String? = null,
    @SerialName("Title")
    val title: String? = null,
    @SerialName("DisplayTitle")
    val displayTitle: String? = null,
    @SerialName("IsDefault")
    val isDefault: Boolean = false,
    @SerialName("IsForced")
    val isForced: Boolean = false,
    @SerialName("IsExternal")
    val isExternal: Boolean = false,
    @SerialName("BitRate")
    val bitRate: Int? = null,
    @SerialName("Width")
    val width: Int? = null,
    @SerialName("Height")
    val height: Int? = null,
    @SerialName("AspectRatio")
    val aspectRatio: String? = null,
    @SerialName("FrameRate")
    val frameRate: Float? = null,
    @SerialName("Channels")
    val channels: Int? = null,
    @SerialName("SampleRate")
    val sampleRate: Int? = null,
    @SerialName("ChannelLayout")
    val channelLayout: String? = null
)