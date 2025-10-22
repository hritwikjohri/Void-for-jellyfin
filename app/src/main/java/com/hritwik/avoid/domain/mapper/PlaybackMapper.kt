package com.hritwik.avoid.domain.mapper


import com.hritwik.avoid.data.remote.dto.media.MediaSourceDto
import com.hritwik.avoid.data.remote.dto.media.MediaStreamDto
import com.hritwik.avoid.data.remote.dto.playback.SegmentDto
import com.hritwik.avoid.domain.model.media.MediaSource
import com.hritwik.avoid.domain.model.media.MediaStream
import com.hritwik.avoid.domain.model.playback.Segment
import org.jellyfin.sdk.model.api.MediaStreamType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackMapper @Inject constructor() {

    
    fun mapMediaStreamDtoToMediaStream(dto: MediaStreamDto): MediaStream {
        return MediaStream(
            index = dto.index,
            type = MediaStreamType.fromName(dto.type),
            codec = dto.codec,
            language = dto.language,
            displayLanguage = dto.displayLanguage,
            title = dto.title,
            displayTitle = dto.displayTitle,
            isDefault = dto.isDefault,
            isForced = dto.isForced,
            isExternal = dto.isExternal,
            bitRate = dto.bitRate,
            width = dto.width,
            height = dto.height,
            aspectRatio = dto.aspectRatio,
            frameRate = dto.frameRate,
            channels = dto.channels,
            sampleRate = dto.sampleRate,
            channelLayout = dto.channelLayout
        )
    }

    
    fun mapMediaSourceDtoToMediaSource(dto: MediaSourceDto): MediaSource {
        return MediaSource(
            id = dto.id,
            name = dto.name,
            type = dto.type,
            container = dto.container,
            size = dto.size,
            bitrate = dto.bitrate,
            path = dto.path,
            protocol = dto.protocol,
            runTimeTicks = dto.runTimeTicks,
            videoType = dto.videoType,
            mediaStreams = dto.mediaStreams.map { mapMediaStreamDtoToMediaStream(it) },
            isRemote = dto.isRemote,
            supportsTranscoding = dto.supportsTranscoding,
            supportsDirectStream = dto.supportsDirectStream,
            supportsDirectPlay = dto.supportsDirectPlay
        )
    }

    
    fun mapMediaSourceDtoListToMediaSourceList(dtoList: List<MediaSourceDto>): List<MediaSource> {
        return dtoList.map { mapMediaSourceDtoToMediaSource(it) }
    }

    
    fun mapMediaStreamDtoListToMediaStreamList(dtoList: List<MediaStreamDto>): List<MediaStream> {
        return dtoList.map { mapMediaStreamDtoToMediaStream(it) }
    }

    
    fun mapSegmentDtoToSegment(dto: SegmentDto): Segment {
        return Segment(
            id = dto.id,
            startPositionTicks = dto.startTicks,
            endPositionTicks = dto.endTicks,
            type = dto.type
        )
    }

    
    fun mapSegmentDtoListToSegmentList(dtoList: List<SegmentDto>): List<Segment> {
        return dtoList.map { mapSegmentDtoToSegment(it) }
    }
}