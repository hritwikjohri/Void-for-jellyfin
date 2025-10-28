package com.hritwik.avoid.utils.constants

object MpvConstants {
    const val CONFIG_DIRECTORY = "mpv"
    const val CONFIG_FILE_NAME = "mpv.conf"

    val DEFAULT_CONFIG: String = listOf(
        "hwdec=auto",
        "vd-lavc-software-fallback=yes",
        "cache=yes",
        "demuxer-max-bytes=${100 * 1024 * 1024}",
        "vd-lavc-assume-old-x264=yes",
        "codec-profile=custom",
        "vd-lavc-check-hw-profile=no",
        "vd-lavc-codec-whitelist=h264,hevc,vp8,vp9,av1,mpeg2video,mpeg4",
        "sub-codepage=auto",
        "sub-fix-timing=yes",
        "blend-subtitles=yes",
        "sub-forced-only=no"
    ).joinToString(separator = "\n")
}
