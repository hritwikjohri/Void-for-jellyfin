package com.hritwik.avoid.domain.model.download

data class DownloadRequest(
    val quality: DownloadQuality,
    val static: Boolean,
    val maxWidth: Int? = null,
    val maxHeight: Int? = null,
    val maxBitrate: Int? = null,
    val videoBitrate: Int? = null,
    val audioBitrate: Int? = null,
    val videoCodec: DownloadCodec = DownloadCodec.H264,
    val audioCodec: String = DEFAULT_AUDIO_CODECS,
    val copySubtitles: Boolean = true,
    val copyFontData: Boolean = true,
    val enableAutoStreamCopy: Boolean = true,
    val allowVideoStreamCopy: Boolean = true,
    val allowAudioStreamCopy: Boolean = true,
) {
    companion object {
        private const val DEFAULT_AUDIO_CODECS = "aac,mp3,ac3,opus,flac"

        fun defaultAudioCodecs(): String = DEFAULT_AUDIO_CODECS

        fun directDownload(
            quality: DownloadQuality,
            codec: DownloadCodec,
        ): DownloadRequest = DownloadRequest(
            quality = quality,
            static = true,
            maxHeight = quality.height,
            maxBitrate = quality.maxBitrate,
            videoCodec = codec,
            enableAutoStreamCopy = true,
            allowVideoStreamCopy = true,
            allowAudioStreamCopy = true,
        )

        fun transcodedDownload(
            quality: DownloadQuality,
            codec: DownloadCodec,
            maxWidth: Int,
            maxHeight: Int,
            videoBitrate: Int,
        ): DownloadRequest = DownloadRequest(
            quality = quality,
            static = false,
            maxWidth = maxWidth,
            maxHeight = maxHeight,
            maxBitrate = null,
            videoBitrate = videoBitrate,
            audioBitrate = null,
            videoCodec = codec,
            enableAutoStreamCopy = false,
            allowVideoStreamCopy = false,
            allowAudioStreamCopy = false,
        )
    }
}
