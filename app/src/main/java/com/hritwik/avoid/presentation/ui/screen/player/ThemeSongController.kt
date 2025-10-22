package com.hritwik.avoid.presentation.ui.screen.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.hritwik.avoid.presentation.ui.state.ThemeSongState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@OptIn(UnstableApi::class)
class ThemeSongController @Inject constructor(
    @ApplicationContext context: Context,
    cacheDataSourceFactory: CacheDataSource.Factory
) : DefaultLifecycleObserver {

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
        .build()

    private var currentSongId: String? = null
    private var currentSongUrl: String? = null
    private var resumeOnForeground: Boolean = false

    init {
        ProcessLifecycleOwner.Companion.get().lifecycle.addObserver(this)
    }

    fun playThemeSong(themeSong: ThemeSongState, force: Boolean = false) {
        val newId = themeSong.id
        val newUrl = themeSong.url
        if (!force && currentSongId == newId) {
            if (!player.isPlaying) {
                player.playWhenReady = true
            }
            return
        }
        currentSongId = newId
        currentSongUrl = newUrl
        player.setMediaItem(MediaItem.fromUri(newUrl))
        player.prepare()
        player.playWhenReady = true
    }

    fun stopThemeSong(clearSong: Boolean = true) {
        if (clearSong) {
            currentSongId = null
            currentSongUrl = null
            resumeOnForeground = false
        }
        player.stop()
    }

    override fun onStop(owner: LifecycleOwner) {
        if (currentSongId != null) {
            stopThemeSong(clearSong = false)
            resumeOnForeground = true
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        if (resumeOnForeground && currentSongId != null && currentSongUrl != null) {
            playThemeSong(ThemeSongState(currentSongId!!, currentSongUrl!!), force = true)
        }
        resumeOnForeground = false
    }

    fun currentThemeSong(): ThemeSongState? {
        val id = currentSongId
        val url = currentSongUrl
        return if (id != null && url != null) ThemeSongState(id, url) else null
    }

    fun clear() {
        stopThemeSong(clearSong = true)
    }

    fun release() {
        ProcessLifecycleOwner.Companion.get().lifecycle.removeObserver(this)
        player.release()
    }
}