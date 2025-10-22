package com.hritwik.avoid.domain.model.media

data class PlaybackOptions(
    val selectedMediaSource: MediaSource? = null,
    val selectedVideoStream: MediaStream? = null,
    val selectedAudioStream: MediaStream? = null,
    val selectedSubtitleStream: MediaStream? = null,
    val resumePositionTicks: Long = 0L,
    val startFromBeginning: Boolean = false
) {
    
    val isPlaybackReady: Boolean
        get() = selectedMediaSource != null && selectedVideoStream != null && selectedAudioStream != null

    
    val currentVideoQuality: VideoQuality?
        get() = selectedVideoStream?.videoQuality

    
    val shouldShowResume: Boolean
        get() = resumePositionTicks > 0 && !startFromBeginning
}