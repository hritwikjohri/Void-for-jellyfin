package com.hritwik.avoid.domain.model.download

enum class DownloadQuality(val height: Int, val maxBitrate: Int, val label: String) {
    FHD_1080(1080, 8_000_000, "1080p"),
    HD_720(720, 5_000_000, "720p"),
    SD_480(480, 2_500_000, "480p");
}