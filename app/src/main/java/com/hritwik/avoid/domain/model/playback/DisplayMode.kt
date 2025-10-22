package com.hritwik.avoid.domain.model.playback

enum class DisplayMode(val value: String, val description: String) {
    FIT_SCREEN("Fit Screen", "Maintain aspect ratio, fit within screen"),
    CROP("Crop", "Fill screen, may trim edges"),
    STRETCH("Stretch", "Fill screen, ignore aspect ratio"),
    ORIGINAL("Original", "Play in original size");

    companion object {
        fun fromValue(value: String): DisplayMode = DisplayMode.entries.firstOrNull {
            it.value.equals(value, ignoreCase = true)
        } ?: FIT_SCREEN
    }
}