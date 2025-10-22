package com.hritwik.avoid.domain.model.library

import org.junit.Assert.assertEquals
import org.junit.Test
import com.hritwik.avoid.utils.extensions.getPosterUrl

class MediaItemImageUrlTest {
    @Test
    fun posterUrl_returns_file_uri_when_absolute() {
        val path = "/tmp/poster.jpg"
        val item = MediaItem(
            id = "1",
            name = "Test",
            type = "Movie",
            overview = null,
            year = null,
            communityRating = null,
            runTimeTicks = null,
            primaryImageTag = path,
            thumbImageTag = null,
            logoImageTag = null,
            backdropImageTags = emptyList(),
            genres = emptyList(),
            isFolder = false,
            childCount = null,
            userData = null
        )
        val url = item.getPosterUrl("")
        assertEquals(java.io.File(path).toURI().toString(), url)
    }

    @Test
    fun logoUrl_returns_file_uri_when_absolute() {
        val path = "/tmp/logo.png"
        val item = MediaItem(
            id = "1",
            name = "Test",
            type = "Movie",
            overview = null,
            year = null,
            communityRating = null,
            runTimeTicks = null,
            primaryImageTag = null,
            thumbImageTag = null,
            logoImageTag = path,
            backdropImageTags = emptyList(),
            genres = emptyList(),
            isFolder = false,
            childCount = null,
            userData = null
        )
        val url = item.getLogoUrl("")
        assertEquals(java.io.File(path).toURI().toString(), url)
    }
}

