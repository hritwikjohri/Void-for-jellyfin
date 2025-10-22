package com.hritwik.avoid.domain.model.playback

data class PlaybackStartInfo(
    val itemId: String,
    val mediaSourceId: String,
    val playMethod: String,
    val canSeek: Boolean,
    val audioStreamIndex: Int? = null,
    val subtitleStreamIndex: Int? = null
)