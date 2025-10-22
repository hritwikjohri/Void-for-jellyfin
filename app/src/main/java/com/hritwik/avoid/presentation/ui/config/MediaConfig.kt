package com.hritwik.avoid.presentation.ui.config

import com.hritwik.avoid.presentation.ui.state.HeroStyle

data class MediaConfig(
    val heroStyle: HeroStyle,
    val backgroundImageUrl: String?,
    val heroHeight: Int,
    val showShareButton: Boolean = false,
    val playButtonSize: Int,
    val showMediaInfo: Boolean = true,
    val overviewTitle: String,
    val similarSectionTitle: String
)