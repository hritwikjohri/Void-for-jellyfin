package com.hritwik.avoid.domain.model.playback

enum class PlayerType(val value: String, val description: String) {
    MPV("MPV", "Use the MPV-based player"),
    EXOPLAYER("ExoPlayer", "Use the ExoPlayer-based player");

    companion object {
        fun fromValue(value: String): PlayerType = entries.firstOrNull {
            it.value.equals(value, ignoreCase = true)
        } ?: MPV
    }
}
