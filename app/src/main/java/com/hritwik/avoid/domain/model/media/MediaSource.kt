package com.hritwik.avoid.domain.model.media

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.jellyfin.sdk.model.api.MediaStreamType
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class MediaSource(
    val id: String,
    val name: String?,
    val type: String?,
    val container: String?,
    val size: Long?,
    val bitrate: Int?,
    val path: String?,
    val protocol: String?,
    val runTimeTicks: Long?,
    val videoType: String?,
    val mediaStreams: List<MediaStream>,
    val isRemote: Boolean,
    val supportsTranscoding: Boolean,
    val supportsDirectStream: Boolean,
    val supportsDirectPlay: Boolean
) : Parcelable {
    val videoStreams: List<MediaStream>
        get() = mediaStreams.filter { it.type == MediaStreamType.VIDEO }

    val audioStreams: List<MediaStream>
        get() = mediaStreams.filter { it.type == MediaStreamType.AUDIO }

    val subtitleStreams: List<MediaStream>
        get() = mediaStreams.filter { it.type == MediaStreamType.SUBTITLE }

    val subtitleOffIndex: Int?
        get() {
            if (subtitleStreams.isEmpty()) return null
            val maxIndex = mediaStreams.maxOfOrNull { it.index }
            return maxIndex?.plus(1) ?: mediaStreams.size
        }

    val defaultVideoStream: MediaStream?
        get() = videoStreams.firstOrNull { it.isDefault } ?: videoStreams.firstOrNull()

    val defaultAudioStream: MediaStream?
        get() = audioStreams.firstOrNull { it.isDefault } ?: audioStreams.firstOrNull()

    val defaultSubtitleStream: MediaStream?
        get() = subtitleStreams.firstOrNull { it.isDefault }

    val availableVideoQualities: List<VideoQuality>
        get() = videoStreams.mapNotNull { it.videoQuality }.distinct()
            .sortedByDescending { it.height }

    val displayName: String
        get() = name ?: "Version ${id.take(8)}"

    fun MediaSource.hasMultipleVideoQualities(): Boolean {
        return videoStreams.distinctBy { it.height }.size > 1
    }

    fun MediaSource.hasMultipleAudioTracks(): Boolean {
        return audioStreams.size > 1
    }

    fun MediaSource.hasSubtitles(): Boolean {
        return subtitleStreams.isNotEmpty()
    }

    fun MediaSource.getBestVideoStream(): MediaStream? {
        return videoStreams.maxByOrNull { it.height ?: 0 }
    }

    fun MediaSource.getBestAudioStream(): MediaStream? {
        return audioStreams.firstOrNull { it.isDefault }
            ?: audioStreams.firstOrNull { it.language == "eng" }
            ?: audioStreams.firstOrNull()
    }

    val versionInfo: String
        get() {
            val n = name
            if (n != null) {
                val tag = "{edition-"
                val open = n.indexOf(tag, ignoreCase = true)
                if (open != -1) {
                    val start = open + tag.length
                    if (start < n.length) {
                        val close = n.indexOf('}', start)
                        if (close > start) {
                            val edition = n.substring(start, close).trim()
                            if (edition.isNotEmpty()) return edition
                        }
                    }
                }
            }

            fun detectFromName(source: String?): String? {
                val s = source ?: return null
                val lower = s.lowercase()

                val sourceChecks = listOf(
                    "remux" to "REMUX",
                    "web-dl" to "WEB-DL",
                    "web dl" to "WEB-DL",
                    "webrip" to "WEBRip",
                    "blu-ray" to "BluRay",
                    "bluray" to "BluRay",
                    "bdrip" to "BDRip",
                    "dvdrip" to "DVDRip",
                    "hdtv" to "HDTV",
                    "cam" to "CAM",
                    "ts" to "TS",
                    "screener" to "SCREENER"
                )
                for ((k, v) in sourceChecks) if (lower.contains(k)) return v

                val resChecks = listOf(
                    "7680p" to "8K",
                    "4320p" to "8K",
                    "8k" to "8K",
                    "2160p" to "4K",
                    "4k" to "4K",
                    "1440p" to "2K",
                    "2k" to "2K",
                    "1080p" to "FHD",
                    "fhd" to "FHD",
                    "720p" to "HD",
                    "hd" to "HD",
                    "480p" to "SD",
                    "360p" to "SD",
                    "240p" to "SD",
                    "sd" to "SD"
                )
                for ((k, v) in resChecks) if (lower.contains(k)) return v

                return null
            }

            detectFromName(n)?.let { return it }

            val height = defaultVideoStream?.height ?: -1
            return when {
                height >= 4320 -> "8K"
                height >= 2160 -> "4K"
                height >= 1440 -> "2K"
                height >= 1080 -> "FHD"
                height >= 720 -> "HD"
                height >= 240 -> "SD"
                height > 0 -> "SD"
                else -> displayName
            }
        }
}
