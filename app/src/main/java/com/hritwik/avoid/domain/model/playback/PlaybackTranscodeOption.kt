package com.hritwik.avoid.domain.model.playback


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

    
    fun appendQueryParameters(
        builder: StringBuilder,
        videoCodecOverride: String? = null,
        audioCodecOverride: String? = null,
        videoCodecProfileOverride: String? = null,
        allowAudioStreamCopyOverride: Boolean? = null,
    ) {
        val resolvedVideoCodec = videoCodecOverride?.takeIf { it.isNotBlank() } ?: videoCodec
        val resolvedVideoProfile = videoCodecProfileOverride?.takeIf { it.isNotBlank() } ?: videoCodecProfile
        val resolvedAudioCodec = audioCodecOverride?.takeIf { it.isNotBlank() } ?: audioCodec
        val resolvedAllowAudioStreamCopy = allowAudioStreamCopyOverride ?: defaultAllowAudioStreamCopy
        resolvedVideoCodec?.let { codec ->
            builder.append("&VideoCodec=").append(codec)
            resolvedVideoProfile?.let { profile ->
                builder.append("&Profile=").append(profile)
            }
        }
        resolvedAudioCodec?.let { builder.append("&AudioCodec=").append(it) }
        maxWidth?.let { builder.append("&MaxWidth=").append(it) }
        maxHeight?.let { builder.append("&MaxHeight=").append(it) }
        maxBitrate?.let {
            builder.append("&MaxBitrate=").append(it)
        }
        videoBitrate?.let { builder.append("&VideoBitrate=").append(it) }
        builder.append("&AllowVideoStreamCopy=").append(allowVideoStreamCopy)
        builder.append("&AllowAudioStreamCopy=").append(resolvedAllowAudioStreamCopy)
        builder.append("&EnableAutoStreamCopy=").append(enableAutoStreamCopy)
    }
}
