package com.hritwik.avoid.data.local.model

import kotlinx.serialization.Serializable

@Serializable
data class PlaybackPreferences(
    val mediaSourceId: String? = null,
    val audioIndex: Int? = null,
    val subtitleIndex: Int? = null,
    val videoQuality: String? = null
)