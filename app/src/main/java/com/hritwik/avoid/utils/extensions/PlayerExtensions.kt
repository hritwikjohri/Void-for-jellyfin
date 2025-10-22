package com.hritwik.avoid.utils.extensions

import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.PlaybackOptions


fun MediaItem.getStreamContainer(mediaSourceId: String? = null): String? {
    val source = mediaSourceId?.let { id ->
        mediaSources.firstOrNull { it.id == id }
    } ?: mediaSources.firstOrNull()

    return source?.container
        ?: source?.mediaStreams?.firstOrNull()?.codec
}


fun MediaItem.getPlaybackUrl(
    serverUrl: String,
    accessToken: String,
    mediaSourceId: String? = null,
    audioStreamIndex: Int? = null,
    subtitleStreamIndex: Int? = null,
    startTimeTicks: Long? = null,
    maxBitrate: Int? = null
): String {
    val source = mediaSourceId ?: this.mediaSources?.firstOrNull()?.id ?: this.id

    return buildString {
        append(serverUrl.removeSuffix("/"))
        append("/Videos/")
        append(id)
        append("/stream")
        append("?MediaSourceId=")
        append(source)
        append("&api_key=")
        append(accessToken)

        audioStreamIndex?.let {
            append("&AudioStreamIndex=")
            append(it)
        }

        subtitleStreamIndex?.let {
            append("&SubtitleStreamIndex=")
            append(it)
        }

        startTimeTicks?.let {
            append("&StartTimeTicks=")
            append(it)
        }

        maxBitrate?.let {
            append("&MaxStreamingBitrate=")
            append(it)
        }

        
        append("&Static=true")
        append("&EnableAutoStreamCopy=true")
        append("&AllowVideoStreamCopy=true")
        append("&AllowAudioStreamCopy=true")
    }
}

fun MediaItem.resolveSubtitleOffIndex(
    mediaSourceId: String?,
    audioStreams: List<MediaStream>,
    subtitleStreams: List<MediaStream>
): Int? {
    if (subtitleStreams.isEmpty()) return null
    val mediaSource = mediaSources.firstOrNull { it.id == mediaSourceId }
        ?: mediaSources.firstOrNull()
    mediaSource?.subtitleOffIndex?.let { return it }
    val maxIndex = (audioStreams + subtitleStreams).maxOfOrNull { it.index } ?: return null
    return maxIndex + 1
}


fun MediaItem.getHlsStreamUrl(
    serverUrl: String,
    accessToken: String,
    mediaSourceId: String? = null,
    maxBitrate: Int? = null
): String {
    val source = mediaSourceId ?: this.mediaSources?.firstOrNull()?.id ?: this.id

    return buildString {
        append(serverUrl.removeSuffix("/"))
        append("/Videos/")
        append(id)
        append("/master.m3u8")
        append("?MediaSourceId=")
        append(source)
        append("&api_key=")
        append(accessToken)

        maxBitrate?.let {
            append("&MaxStreamingBitrate=")
            append(it)
        }

        append("&VideoCodec=h264")
        append("&AudioCodec=aac,mp3")
        append("&TranscodingContainer=ts")
        append("&TranscodingProtocol=hls")
        append("&EnableAutoStreamCopy=true")
    }
}


fun MediaItem.getTranscodingUrl(
    serverUrl: String,
    accessToken: String,
    playbackOptions: PlaybackOptions
): String {
    return buildString {
        append(serverUrl.removeSuffix("/"))
        append("/Videos/")
        append(id)
        append("/stream.mp4") 
        append("?api_key=")
        append(accessToken)

        playbackOptions.selectedMediaSource?.let { source ->
            append("&MediaSourceId=")
            append(source.id)
        }

        playbackOptions.selectedAudioStream?.let { audio ->
            append("&AudioStreamIndex=")
            append(audio.index)
        }

        playbackOptions.selectedSubtitleStream?.let { subtitle ->
            append("&SubtitleStreamIndex=")
            append(subtitle.index)
        }

        playbackOptions.startPositionTicks?.let { ticks ->
            append("&StartTimeTicks=")
            append(ticks)
        }

        
        append("&VideoCodec=h264")
        append("&AudioCodec=aac")
        append("&MaxVideoBitrate=")
        append(playbackOptions.maxBitrate ?: 8000000) 

        
        append("&VideoQuality=")
        append(playbackOptions.videoQuality ?: 100)

        
        append("&EnableHardwareAcceleration=true")
        append("&EnableHardwareEncoding=true")
    }
}


fun MediaItem.getSubtitleUrl(
    serverUrl: String,
    accessToken: String,
    subtitleStreamIndex: Int,
    format: String = "vtt"
): String {
    val mediaSource = this.mediaSources.firstOrNull()
    val mediaSourceId = mediaSource?.id ?: this.id
    val streamCodec = mediaSource?.mediaStreams
        ?.firstOrNull { it.index == subtitleStreamIndex }
        ?.codec
    val format = streamCodec.toSubtitleFileExtension()

    return buildString {
        append(serverUrl.removeSuffix("/"))
        append("/Videos/")
        append(id)
        append("/")
        append(mediaSourceId)
        append("/Subtitles/")
        append(subtitleStreamIndex)
        append("/Stream.")
        append(format)
        append("?api_key=")
        append(accessToken)
    }
}


fun String?.toSubtitleFileExtension(): String = when (this?.lowercase()) {
    "pgs" -> "sup"
    "sub" -> "sub"
    "srt", "subrip" -> "srt"
    "ass" -> "ass"
    "ssa" -> "ssa"
    "webvtt", "wvtt" -> "vtt"
    "dvbsub", "dvb" -> "dvb"
    "eia608", "cea608" -> "cea608"
    "eia708", "cea708" -> "cea708"
    "tx3g", "mp4vtt" -> "mp4vtt"
    else -> this ?: "srt"
}


fun MediaItem.supportsDirectPlay(): Boolean {
    return mediaSources?.any { source ->
        source.supportsDirectPlay == true
    } ?: false
}


fun MediaItem.supportsDirectStream(): Boolean {
    return mediaSources?.any { source ->
        source.supportsDirectStream == true
    } ?: false
}


fun MediaItem.getBestMediaSource(): MediaSource? {
    return mediaSources?.firstOrNull { it.supportsDirectPlay == true }
        ?: mediaSources?.firstOrNull { it.supportsDirectStream == true }
        ?: mediaSources?.firstOrNull()
}


fun MediaItem.getResumePositionMs(): Long {
    val ticks = userData?.playbackPositionTicks ?: 0L
    return ticks / 10_000 
}


fun MediaItem.isPartiallyWatched(): Boolean {
    val playbackPosition = userData?.playbackPositionTicks ?: 0L
    val runtime = runTimeTicks ?: 0L

    return playbackPosition > 0 && playbackPosition < runtime * 0.9
}


fun MediaItem.getPlaybackProgressPercent(): Int {
    val position = userData?.playbackPositionTicks ?: return 0
    val runtime = runTimeTicks ?: return 0

    if (runtime == 0L) return 0

    return ((position.toDouble() / runtime.toDouble()) * 100).toInt().coerceIn(0, 100)
}


fun buildVideoUrlWithOptions(
    serverUrl: String,
    accessToken: String,
    mediaItem: MediaItem,
    mediaSourceId: String? = null,
    audioStreamIndex: Int? = null,
    subtitleStreamIndex: Int? = null
): String {
    
    val sourceId = mediaSourceId
        ?: mediaItem.mediaSources.firstOrNull()?.id
        ?: mediaItem.id

    return buildString {
        append(serverUrl.removeSuffix("/"))
        append("/Videos/")
        append(mediaItem.id)
        append("/stream")
        append("?MediaSourceId=")
        append(sourceId)
        append("&api_key=")
        append(accessToken)

        
        append("&Static=true")
        append("&EnableAutoStreamCopy=true")
        append("&AllowVideoStreamCopy=true")
        append("&AllowAudioStreamCopy=true")

        
        audioStreamIndex?.let { index ->
            if (index >= 0) {
                append("&AudioStreamIndex=")
                append(index)
            }
        }

        
        subtitleStreamIndex?.let { index ->
            if (index >= 0) {
                append("&SubtitleStreamIndex=")
                append(index)
                append("&SubtitleMethod=Encode")
            }
        }
    }
}


fun buildVideoUrlWithTranscoding(
    serverUrl: String,
    accessToken: String,
    mediaItem: MediaItem,
    mediaSourceId: String? = null,
    audioStreamIndex: Int? = null,
    subtitleStreamIndex: Int? = null,
    maxBitrate: Int? = null
): String {
    val sourceId = mediaSourceId
        ?: mediaItem.mediaSources?.firstOrNull()?.id
        ?: mediaItem.id

    return buildString {
        append(serverUrl.removeSuffix("/"))
        append("/Videos/")
        append(mediaItem.id)
        append("/stream")
        append("?MediaSourceId=")
        append(sourceId)
        append("&api_key=")
        append(accessToken)
        append("&Static=true")
        append("&EnableAutoStreamCopy=true")
        append("&AllowVideoStreamCopy=true")
        append("&AllowAudioStreamCopy=true")

        
        audioStreamIndex?.let { index ->
            if (index >= 0) {
                append("&AudioStreamIndex=")
                append(index)
            }
        }

        
        subtitleStreamIndex?.let { index ->
            if (index >= 0) {
                append("&SubtitleStreamIndex=")
                append(index)
            }
        }

        
        maxBitrate?.let {
            append("&VideoBitrate=")
            append(it)
        }

        
        append("&VideoCodec=h264")
        append("&AudioCodec=aac,mp3,ac3")
        append("&TranscodingMaxAudioChannels=6")

        
        val container = mediaItem.mediaSources?.firstOrNull()?.container
        container?.let {
            append("&Container=")
            append(it)
        }
    }
}

fun buildVideoUrl(
    serverUrl: String,
    accessToken: String,
    mediaItem: MediaItem,
    container: String = "mkv"
): String {
    val mediaSourceId = mediaItem.mediaSources?.firstOrNull()?.id ?: mediaItem.id

    return buildString {
        append(serverUrl.removeSuffix("/"))
        append("/Videos/")
        append(mediaItem.id)
        append("/stream")
        append("?MediaSourceId=")
        append(mediaSourceId)
        append("&api_key=")
        append(accessToken)
        append("&Static=true")
        append("&EnableAutoStreamCopy=true")
        append("&AllowVideoStreamCopy=true")
        append("&AllowAudioStreamCopy=true")
        append("&Container=")
        append(container)
    }
}