package com.hritwik.avoid.domain.model.playback

enum class PreferredVideoCodec(
    val label: String,
    val preferenceValue: String,
    val codecQueryValue: String,
    val profile: String? = null,
) {
    H264(
        label = "H.264",
        preferenceValue = "h264",
        codecQueryValue = "h264",
    ),
    HEVC(
        label = "HEVC",
        preferenceValue = "hevc",
        codecQueryValue = "hevc",
    ),
    HEVC_MAIN10(
        label = "HEVC Main10",
        preferenceValue = "hevc_main10",
        codecQueryValue = "hevc",
        profile = "main10",
    ),
    AV1(
        label = "AV1",
        preferenceValue = "av1",
        codecQueryValue = "av1",
    );

    companion object {
        fun fromPreferenceValue(value: String?): PreferredVideoCodec {
            if (value.isNullOrBlank()) return H264
            return entries.firstOrNull {
                it.preferenceValue.equals(value, ignoreCase = true) ||
                    it.codecQueryValue.equals(value, ignoreCase = true)
            } ?: H264
        }
    }
}
