package com.hritwik.avoid.domain.model.playback


data class TranscodeRequestParameters(
    val videoCodec: String?,
    val videoCodecProfile: String?,
    val audioCodec: String?,
    val maxWidth: Int?,
    val maxHeight: Int?,
    val maxBitrate: Int?,
    val videoBitrate: Int?,
    val allowVideoStreamCopy: Boolean,
    val allowAudioStreamCopy: Boolean,
    val enableAutoStreamCopy: Boolean,
)


enum class PlaybackTranscodeOption(
    val label: String,
    val isStaticStream: Boolean,
    private val maxWidth: Int?,
    private val maxHeight: Int?,
    private val maxBitrate: Int?,
    private val videoBitrate: Int?,
    private val videoCodec: String?,
    private val videoCodecProfile: String? = null,
    private val audioCodec: String?,
    private val allowVideoStreamCopy: Boolean,
    private val defaultAllowAudioStreamCopy: Boolean,
    private val enableAutoStreamCopy: Boolean,
) {
    ORIGINAL(
        label = "Original",
        isStaticStream = true,
        maxWidth = null,
        maxHeight = null,
        maxBitrate = null,
        videoBitrate = null,
        videoCodec = null,
        audioCodec = null,
        allowVideoStreamCopy = true,
        defaultAllowAudioStreamCopy = true,
        enableAutoStreamCopy = true,
    ),
    FHD_1080(
        label = "1080p",
        isStaticStream = false,
        maxWidth = 1920,
        maxHeight = 1080,
        maxBitrate = null,
        videoBitrate = 10_000_000,
        videoCodec = "h264",
        audioCodec = null,
        allowVideoStreamCopy = false,
        defaultAllowAudioStreamCopy = false,
        enableAutoStreamCopy = false,
    ),
    HD_720(
        label = "720p",
        isStaticStream = false,
        maxWidth = 1280,
        maxHeight = 720,
        maxBitrate = null,
        videoBitrate = 6_000_000,
        videoCodec = "h264",
        audioCodec = null,
        allowVideoStreamCopy = false,
        defaultAllowAudioStreamCopy = false,
        enableAutoStreamCopy = false,
    );

    val isOriginal: Boolean
        get() = this == ORIGINAL

    val displayHeight: Int?
        get() = maxHeight

    val displayBitrate: Int?
        get() = videoBitrate ?: maxBitrate

    
    fun resolveParameters(
        videoCodecOverride: String? = null,
        audioCodecOverride: String? = null,
        videoCodecProfileOverride: String? = null,
        allowAudioStreamCopyOverride: Boolean? = null,
    ): TranscodeRequestParameters {
        val resolvedVideoCodec = videoCodecOverride?.takeIf { it.isNotBlank() } ?: videoCodec
        val resolvedVideoProfile = videoCodecProfileOverride?.takeIf { it.isNotBlank() } ?: videoCodecProfile
        val resolvedAudioCodec = audioCodecOverride?.takeIf { it.isNotBlank() } ?: audioCodec
        val resolvedAllowAudioStreamCopy = allowAudioStreamCopyOverride ?: defaultAllowAudioStreamCopy
        return TranscodeRequestParameters(
            videoCodec = resolvedVideoCodec,
            videoCodecProfile = resolvedVideoProfile,
            audioCodec = resolvedAudioCodec,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            maxBitrate = maxBitrate,
            videoBitrate = videoBitrate,
            allowVideoStreamCopy = allowVideoStreamCopy,
            allowAudioStreamCopy = resolvedAllowAudioStreamCopy,
            enableAutoStreamCopy = enableAutoStreamCopy,
        )
    }


    fun appendQueryParameters(
        builder: StringBuilder,
        videoCodecOverride: String? = null,
        audioCodecOverride: String? = null,
        videoCodecProfileOverride: String? = null,
        allowAudioStreamCopyOverride: Boolean? = null,
    ) {
        val parameters = resolveParameters(
            videoCodecOverride = videoCodecOverride,
            audioCodecOverride = audioCodecOverride,
            videoCodecProfileOverride = videoCodecProfileOverride,
            allowAudioStreamCopyOverride = allowAudioStreamCopyOverride,
        )
        parameters.videoCodec?.let { codec ->
            builder.append("&VideoCodec=").append(codec)
            parameters.videoCodecProfile?.let { profile ->
                builder.append("&Profile=").append(profile)
            }
        }
        parameters.audioCodec?.let { builder.append("&AudioCodec=").append(it) }
        parameters.maxWidth?.let { builder.append("&MaxWidth=").append(it) }
        parameters.maxHeight?.let { builder.append("&MaxHeight=").append(it) }
        parameters.maxBitrate?.let { builder.append("&MaxBitrate=").append(it) }
        parameters.videoBitrate?.let { builder.append("&VideoBitrate=").append(it) }
        builder.append("&AllowVideoStreamCopy=").append(parameters.allowVideoStreamCopy)
        builder.append("&AllowAudioStreamCopy=").append(parameters.allowAudioStreamCopy)
        builder.append("&EnableAutoStreamCopy=").append(parameters.enableAutoStreamCopy)
    }
}
