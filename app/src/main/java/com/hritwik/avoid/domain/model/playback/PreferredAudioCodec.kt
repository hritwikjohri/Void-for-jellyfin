package com.hritwik.avoid.domain.model.playback

enum class PreferredAudioCodec(
    val label: String,
    val preferenceValue: String,
) {
    ORIGINAL(label = "Original", preferenceValue = "original"),
    OPUS(label = "Opus", preferenceValue = "opus"),
    AAC(label = "AAC", preferenceValue = "aac"),
    FLAC(label = "FLAC", preferenceValue = "flac");

    companion object {
        fun fromPreferenceValue(value: String?): PreferredAudioCodec {
            if (value.isNullOrBlank()) return AAC
            return entries.firstOrNull { it.preferenceValue.equals(value, ignoreCase = true) } ?: AAC
        }
    }
}
