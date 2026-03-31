package com.hritwik.avoid.domain.model.playback

enum class PlayerType(val value: String) {
    AUTO("Auto"),
    MPV("MPV"),
    EXOPLAYER("ExoPlayer");

    companion object {
        fun fromValue(value: String): PlayerType = entries.firstOrNull {
            it.value.equals(value, ignoreCase = true)
        } ?: MPV
    }
}
