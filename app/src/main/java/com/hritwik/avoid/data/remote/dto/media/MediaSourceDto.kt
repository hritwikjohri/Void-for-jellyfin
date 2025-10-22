package com.hritwik.avoid.data.remote.dto.media

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MediaSourceDto(
    @SerialName("Id")
    val id: String,
    @SerialName("Name")
    val name: String? = null,
    @SerialName("Type")
    val type: String? = null,
    @SerialName("Container")
    val container: String? = null,
    @SerialName("Size")
    val size: Long? = null,
    @SerialName("Bitrate")
    val bitrate: Int? = null,
    @SerialName("Path")
    val path: String? = null,
    @SerialName("Protocol")
    val protocol: String? = null,
    @SerialName("RunTimeTicks")
    val runTimeTicks: Long? = null,
    @SerialName("VideoType")
    val videoType: String? = null,
    @SerialName("MediaStreams")
    val mediaStreams: List<MediaStreamDto> = emptyList(),
    @SerialName("IsRemote")
    val isRemote: Boolean = false,
    @SerialName("SupportsTranscoding")
    val supportsTranscoding: Boolean = false,
    @SerialName("SupportsDirectStream")
    val supportsDirectStream: Boolean = false,
    @SerialName("SupportsDirectPlay")
    val supportsDirectPlay: Boolean = false
)