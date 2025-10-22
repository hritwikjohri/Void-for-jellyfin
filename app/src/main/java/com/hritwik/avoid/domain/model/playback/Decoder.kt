package com.hritwik.avoid.domain.model.playback

enum class DecoderMode(val value: String, val description: String) {
    AUTO("Auto", "Let the system decide automatically"),
    HARDWARE_ONLY("Hardware Only", "Maximum performance, limited compatibility"),
    SOFTWARE_ONLY("Software Only", "Universal compatibility, higher CPU usage");

    companion object {
        fun fromValue(value: String): DecoderMode = DecoderMode.entries.firstOrNull {
            it.value.equals(value, ignoreCase = true)
        } ?: AUTO
    }
}