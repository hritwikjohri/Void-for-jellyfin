package com.hritwik.avoid.domain.model.media

import org.jellyfin.sdk.model.api.MediaStreamType
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaStreamTest {

    private fun createMediaStream(width: Int?, height: Int?) = MediaStream(
        index = 0,
        type = MediaStreamType.VIDEO,
        codec = null,
        language = null,
        displayLanguage = null,
        title = null,
        displayTitle = null,
        isDefault = false,
        isForced = false,
        isExternal = false,
        bitRate = null,
        width = width,
        height = height,
        aspectRatio = null,
        frameRate = null,
        channels = null,
        sampleRate = null,
        channelLayout = null,
    )

    @Test
    fun `videoQuality favors width when available`() {
        val stream = createMediaStream(width = 1920, height = 2160)

        assertEquals(VideoQuality.FHD_1080P, stream.videoQuality)
    }

    @Test
    fun `videoQuality falls back to height when width missing`() {
        val stream = createMediaStream(width = null, height = 2160)

        assertEquals(VideoQuality.UHD_4K, stream.videoQuality)
    }

    @Test
    fun `videoQuality defaults to SD when no dimensions are provided`() {
        val stream = createMediaStream(width = null, height = null)

        assertEquals(VideoQuality.SD_480P, stream.videoQuality)
    }
}
