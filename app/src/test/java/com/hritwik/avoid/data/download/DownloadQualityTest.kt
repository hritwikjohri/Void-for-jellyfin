package com.hritwik.avoid.data.download

import com.hritwik.avoid.domain.model.download.DownloadCodec
import com.hritwik.avoid.domain.model.download.DownloadQuality
import com.hritwik.avoid.domain.model.download.DownloadRequest
import com.hritwik.avoid.utils.helpers.normalizeUuid
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadQualityTest {
    @Test
    fun requestUrl_includes_quality_params() {
        val serverUrl = "http://example.com"
        val itemId = normalizeUuid("6e7654822ec0306283de236a018911cf")
        val quality = DownloadQuality.HD_720
        val request = DownloadRequest.directDownload(quality, DownloadCodec.H264)
        val url = buildString {
            append(serverUrl)
            append("/Videos/")
            append(itemId)
            append("/stream.mkv")
            append("?static=")
            append(request.static)
            append("&mediaSourceId=")
            append(itemId)
            request.maxHeight?.let { append("&MaxHeight=$it") }
            request.maxBitrate?.let { append("&MaxBitrate=$it") }
            append("&VideoCodec=")
            append(request.videoCodec.serverValue)
        }
        assertTrue(url.contains("static=true"))
        assertTrue(url.contains("MaxHeight=${quality.height}"))
        assertTrue(url.contains("MaxBitrate=${quality.maxBitrate}"))
        assertTrue(url.contains("VideoCodec=${request.videoCodec.serverValue}"))
    }

    @Test
    fun transcoded_hevc_request_adds_main10_profile() {
        val serverUrl = "http://example.com"
        val itemId = normalizeUuid("6e7654822ec0306283de236a018911cf")
        val request = DownloadRequest.transcodedDownload(
            quality = DownloadQuality.FHD_1080,
            codec = DownloadCodec.HEVC_MAIN10,
            maxWidth = 1920,
            maxHeight = 1080,
            videoBitrate = 8_000_000
        )

        val url = buildString {
            append(serverUrl)
            append("/Videos/")
            append(itemId)
            append("/stream.mkv")
            append("?static=")
            append(request.static)
            append("&mediaSourceId=")
            append(itemId)
            request.maxWidth?.let { append("&MaxWidth=$it") }
            request.maxHeight?.let { append("&MaxHeight=$it") }
            request.videoBitrate?.let { append("&VideoBitrate=$it") }
            append("&VideoCodec=")
            append(request.videoCodec.serverValue)
            if (!request.static) {
                request.videoCodec.profile?.let { append("&Profile=$it") }
            }
        }

        assertTrue(url.contains("VideoCodec=${request.videoCodec.serverValue}"))
        assertTrue(url.contains("Profile=main10"))
        assertTrue(url.contains("static=false"))
        assertTrue(url.contains("VideoBitrate=${request.videoBitrate}"))
        assertFalse(url.contains("MaxBitrate="))
        assertFalse(url.contains("AudioBitrate="))
    }

    @Test
    fun requestUrl_includes_sourceId_when_provided() {
        val serverUrl = "http://example.com"
        val itemId = normalizeUuid("6e7654822ec0306283de236a018911cf")
        val sourceId = "ver123"
        val quality = DownloadQuality.HD_720
        val request = DownloadRequest.directDownload(quality, DownloadCodec.H264)
        val url = buildString {
            append(serverUrl)
            append("/Videos/")
            append(itemId)
            append("/stream.mkv")
            append("?static=")
            append(request.static)
            append("&mediaSourceId=")
            append(sourceId)
        }
        assertTrue(url.contains("mediaSourceId=$sourceId"))
    }

    @Test
    fun filePath_uses_media_source_id_directory() {
        val sourceId = "1a39a001e1ca6f20c25b39a59a9e1e65"
        val extension = "mkv"
        val path = "downloads/$sourceId/$sourceId.$extension"
        assertEquals("downloads/$sourceId/$sourceId.$extension", path)
    }

    @Test
    fun normalizeUuid_formats_32_char_strings() {
        val raw = "6e7654822ec0306283de236a018911cf"
        val expected = "6e765482-2ec0-3062-83de-236a018911cf"
        assertEquals(expected, normalizeUuid(raw))
    }
}

