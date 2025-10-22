package com.hritwik.avoid.domain.model.download

enum class DownloadCodec(
    val label: String,
    val preferenceValue: String,
    val serverValue: String,
    val profile: String? = null,
) {
    H264(
        label = "H.264",
        preferenceValue = "h264",
        serverValue = "h264",
    ),
    HEVC(
        label = "HEVC",
        preferenceValue = "hevc",
        serverValue = "hevc",
    ),
    HEVC_MAIN10(
        label = "HEVC Main10",
        preferenceValue = "hevc_main10",
        serverValue = "hevc",
        profile = "main10",
    ),
    AV1(
        label = "AV1",
        preferenceValue = "av1",
        serverValue = "av1",
    );

    companion object {
        fun fromLabel(label: String): DownloadCodec =
            entries.firstOrNull { it.label.equals(label, ignoreCase = true) }
                ?: entries.firstOrNull { it.preferenceValue.equals(label, ignoreCase = true) }
                ?: entries.firstOrNull { it.serverValue.equals(label, ignoreCase = true) }
                ?: H264
    }
}
