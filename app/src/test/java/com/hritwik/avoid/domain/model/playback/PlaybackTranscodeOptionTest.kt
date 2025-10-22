package com.hritwik.avoid.domain.model.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTranscodeOptionTest {
    @Test
    fun appendQueryParameters_does_not_add_profile_without_override() {
        val builder = StringBuilder()

        PlaybackTranscodeOption.FHD_1080.appendQueryParameters(
            builder,
            videoCodecOverride = "hevc"
        )

        val result = builder.toString()
        assertTrue(result.contains("&VideoCodec=hevc"))
        assertFalse(result.contains("&Profile="))
    }

    @Test
    fun appendQueryParameters_adds_profile_when_override_specified() {
        val builder = StringBuilder()

        PlaybackTranscodeOption.FHD_1080.appendQueryParameters(
            builder,
            videoCodecOverride = "hevc",
            videoCodecProfileOverride = "main10"
        )

        val result = builder.toString()
        assertTrue(result.contains("&VideoCodec=hevc"))
        assertTrue(result.contains("&Profile=main10"))
    }

    @Test
    fun appendQueryParameters_defaults_audio_copy_false_for_transcodes() {
        val builder = StringBuilder()

        PlaybackTranscodeOption.FHD_1080.appendQueryParameters(builder)

        val result = builder.toString()
        assertTrue(result.contains("&AllowAudioStreamCopy=false"))
    }

    @Test
    fun appendQueryParameters_overrides_audio_copy_when_requested() {
        val builder = StringBuilder()

        PlaybackTranscodeOption.FHD_1080.appendQueryParameters(
            builder,
            allowAudioStreamCopyOverride = true,
        )

        val result = builder.toString()
        assertTrue(result.contains("&AllowAudioStreamCopy=true"))
    }
}
