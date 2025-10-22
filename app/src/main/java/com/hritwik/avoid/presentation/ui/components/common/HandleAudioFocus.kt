package com.hritwik.avoid.presentation.ui.components.common

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberAudioFocusRequest(audioManager: AudioManager): AudioFocusRequest {
    val audioAttributes = remember {
        AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
    }
    return remember(audioManager) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes)
            .build()
    }
}