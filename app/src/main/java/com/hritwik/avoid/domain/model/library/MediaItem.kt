package com.hritwik.avoid.domain.model.library

import android.os.Parcelable
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.media.VideoQuality
import com.hritwik.avoid.utils.constants.ApiConstants
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class MediaItem(
    val id: String,
    val name: String,
    val title: String? = null,
    val type: String,
    val overview: String?,
    val year: Int?,
    val communityRating: Double?,
    val runTimeTicks: Long?,
    val primaryImageTag: String?,
    val thumbImageTag: String? = null,
    val logoImageTag: String?,
    val backdropImageTags: List<String>,
    val genres: List<String>,
    val isFolder: Boolean,
    val childCount: Int?,
    val userData: UserData?,
    val taglines: List<String> = emptyList(),
    val people: List<Person> = emptyList(),
    val mediaSources: List<MediaSource> = emptyList(),
    val hasSubtitles: Boolean = false,
    val versionName: String? = null,
    val seriesName: String? = null,
    val seriesId: String? = null,
    val seriesPrimaryImageTag: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val seasonPrimaryImageTag: String? = null,
    val parentIndexNumber: Int? = null,
    val indexNumber: Int? = null
) : Parcelable {
    fun getLogoUrl(serverUrl: String): String? {
        val tag = logoImageTag ?: return null
        val file = java.io.File(tag)
        if (file.isAbsolute) return file.toURI().toString()
        val baseUrl = if (serverUrl.endsWith("/")) serverUrl else "$serverUrl/"
        return "${baseUrl}Items/$id/Images/Logo?tag=$tag&quality=${ApiConstants.DEFAULT_IMAGE_QUALITY}&maxWidth=${ApiConstants.POSTER_MAX_WIDTH}"
    }

    fun getPrimaryTagline(): String? {
        return taglines.firstOrNull()  
    }

    fun hasTaglines(): Boolean {
        return taglines.isNotEmpty()
    }

    
    fun getPrimaryMediaSource(): MediaSource? {
        return mediaSources.firstOrNull()
    }

    
    fun hasMultipleVersions(): Boolean {
        return mediaSources.size > 1
    }

    
    fun getAvailableVideoQualities(): List<VideoQuality> {
        return mediaSources
            .flatMap { it.availableVideoQualities }
            .distinct()
            .sortedByDescending { it.height }
    }

    
    fun getAllAudioStreams(): List<MediaStream> {
        return mediaSources.flatMap { it.audioStreams }
    }

    
    fun getAllSubtitleStreams(): List<MediaStream> {
        return mediaSources.flatMap { it.subtitleStreams }
    }

    
    fun supportsResume(): Boolean {
        return userData?.playbackPositionTicks ?: 0 > 0
    }

    
    fun getResumeTimeDisplay(): String? {
        val positionTicks = userData?.playbackPositionTicks ?: return null
        if (positionTicks <= 0) return null

        val totalTicks = runTimeTicks ?: return null
        val seconds = (positionTicks / 10_000_000).toInt()
        val totalSeconds = (totalTicks / 10_000_000).toInt()

        val minutes = seconds / 60
        val hours = minutes / 60

        return if (hours > 0) {
            "${hours}h ${minutes % 60}m"
        } else {
            "${minutes}m"
        }
    }

    
    fun getBestVideoQualityDescription(): String? {
        val primarySource = getPrimaryMediaSource() ?: return null
        val videoStream = primarySource.defaultVideoStream ?: return null

        return buildString {
            videoStream.videoQuality?.let { append(it.displayName) }
            videoStream.codec?.let {
                if (isNotEmpty()) append(" • ")
                append(it.uppercase())
            }
        }.ifEmpty { null }
    }

    
    fun getAudioTracksCount(): Int {
        return getAllAudioStreams().size
    }

    
    fun getSubtitleTracksCount(): Int {
        return getAllSubtitleStreams().size
    }

    
    fun hasPlaybackDataLoaded(): Boolean {
        return mediaSources.any { it.mediaStreams.isNotEmpty() }
    }
}