package com.hritwik.avoid.domain.model.playback

enum class DisplayMode(val value: String) {
    FIT_SCREEN("Fit Screen"),
    CROP("Crop"),
    STRETCH("Stretch"),
    ORIGINAL("Original");

    companion object {
        fun fromValue(value: String): DisplayMode = DisplayMode.entries.firstOrNull {
            it.value.equals(value, ignoreCase = true)
        } ?: FIT_SCREEN
    }
}