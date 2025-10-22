package com.hritwik.avoid.domain.model.jellyseer

enum class JellyseerVideoQuality(val is4k: Boolean, val displayName: String) {
    HD(is4k = false, displayName = "HD / 1080p"),
    UHD(is4k = true, displayName = "4K / UHD");

    companion object {
        val default: JellyseerVideoQuality = HD
    }
}
