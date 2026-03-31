package com.hritwik.avoid.data.remote.dto.playback

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PlaybackInfoRequestDto(
    @SerialName("MediaSourceId")
    val mediaSourceId: String,
    @SerialName("MaxStreamingBitrate")
    val maxStreamingBitrate: Int? = null,
    @SerialName("EnableDirectPlay")
    val enableDirectPlay: Boolean = false,
    @SerialName("EnableDirectStream")
    val enableDirectStream: Boolean = false,
    @SerialName("EnableTranscoding")
    val enableTranscoding: Boolean = true,
    @SerialName("AllowVideoStreamCopy")
    val allowVideoStreamCopy: Boolean = false,
    @SerialName("AllowAudioStreamCopy")
    val allowAudioStreamCopy: Boolean = false,
    @SerialName("EnableAutoStreamCopy")
    val enableAutoStreamCopy: Boolean = false,
    @SerialName("AlwaysBurnInSubtitleWhenTranscoding")
    val alwaysBurnInSubtitleWhenTranscoding: Boolean = false,
    @SerialName("AudioStreamIndex")
    val audioStreamIndex: Int? = null,
    @SerialName("SubtitleStreamIndex")
    val subtitleStreamIndex: Int? = null,
    @SerialName("StartTimeTicks")
    val startTimeTicks: Long? = null,
    @SerialName("DeviceProfile")
    val deviceProfile: PlaybackDeviceProfileDto,
)

@Serializable
data class PlaybackDeviceProfileDto(
    @SerialName("Name")
    val name: String,
    @SerialName("MaxStreamingBitrate")
    val maxStreamingBitrate: Int? = null,
    @SerialName("DirectPlayProfiles")
    val directPlayProfiles: List<PlaybackDirectPlayProfileDto> = emptyList(),
    @SerialName("TranscodingProfiles")
    val transcodingProfiles: List<PlaybackTranscodingProfileDto> = emptyList(),
    @SerialName("CodecProfiles")
    val codecProfiles: List<PlaybackCodecProfileDto> = emptyList(),
    @SerialName("SubtitleProfiles")
    val subtitleProfiles: List<PlaybackSubtitleProfileDto> = emptyList(),
)

@Serializable
data class PlaybackDirectPlayProfileDto(
    @SerialName("Type")
    val type: String? = null,
    @SerialName("Container")
    val container: String? = null,
)

@Serializable
data class PlaybackTranscodingProfileDto(
    @SerialName("Type")
    val type: String,
    @SerialName("VideoCodec")
    val videoCodec: String? = null,
    @SerialName("AudioCodec")
    val audioCodec: String? = null,
    @SerialName("Profile")
    val profile: String? = null,
    @SerialName("Protocol")
    val protocol: String,
    @SerialName("Context")
    val context: String,
    @SerialName("EnableMpegtsM2TsMode")
    val enableMpegtsM2TsMode: Boolean = true,
    @SerialName("TranscodeSeekInfo")
    val transcodeSeekInfo: String = "Auto",
    @SerialName("CopyTimestamps")
    val copyTimestamps: Boolean = true,
    @SerialName("EnableSubtitlesInManifest")
    val enableSubtitlesInManifest: Boolean = true,
    @SerialName("EnableAudioVbrEncoding")
    val enableAudioVbrEncoding: Boolean = true,
    @SerialName("BreakOnNonKeyFrames")
    val breakOnNonKeyFrames: Boolean = false,
    @SerialName("MaxAudioChannels")
    val maxAudioChannels: Int? = null,
    @SerialName("MaxWidth")
    val maxWidth: Int? = null,
    @SerialName("MaxHeight")
    val maxHeight: Int? = null,
    @SerialName("MaxBitrate")
    val maxBitrate: Int? = null,
    @SerialName("VideoBitrate")
    val videoBitrate: Int? = null,
)

@Serializable
data class PlaybackCodecProfileDto(
    @SerialName("Type")
    val type: String,
    @SerialName("Codec")
    val codec: String,
    @SerialName("Container")
    val container: String,
)

@Serializable
data class PlaybackSubtitleProfileDto(
    @SerialName("Format")
    val format: String,
    @SerialName("Method")
    val method: String,
)

@Serializable
data class PlaybackInfoResponseDto(
    @SerialName("MediaSources")
    val mediaSources: List<PlaybackInfoMediaSourceDto> = emptyList(),
    @SerialName("PlaySessionId")
    val playSessionId: String? = null,
)

@Serializable
data class PlaybackInfoMediaSourceDto(
    @SerialName("Id")
    val id: String? = null,
    @SerialName("TranscodingUrl")
    val transcodingUrl: String? = null,
)
