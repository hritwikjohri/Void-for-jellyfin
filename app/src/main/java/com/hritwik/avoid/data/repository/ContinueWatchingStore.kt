package com.hritwik.avoid.data.repository

import com.hritwik.avoid.data.remote.websocket.PlaybackEvent
import com.hritwik.avoid.domain.model.library.MediaItem
import com.hritwik.avoid.domain.model.library.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds


class ContinueWatchingStore(scope: CoroutineScope) {
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val sortRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 64)

    init {
        scope.launch {
            sortRequests
                .debounce(1.seconds)
                .collect { sortList() }
        }
    }

    suspend fun setInitial(list: List<MediaItem>) {
        mutex.withLock { _items.value = list }
    }

    suspend fun handle(event: PlaybackEvent) {
        when (event) {
            is PlaybackEvent.Progress -> upsert(event)
            is PlaybackEvent.Stop -> removeIfCompleted(event)
        }
    }

    private suspend fun upsert(event: PlaybackEvent.Progress) {
        mutex.withLock {
            val current = _items.value.toMutableList()
            val index = current.indexOfFirst { it.id == event.itemId }
            if (index != -1) {
                val old = current[index]
                val percent = if (event.runTimeTicks > 0) {
                    event.positionTicks.toDouble() / event.runTimeTicks
                } else 0.0
                val updated = old.copy(
                    userData = old.userData?.copy(
                        playbackPositionTicks = event.positionTicks,
                        played = percent >= 0.95,
                        lastPlayedDate = event.datePlayed
                    ) ?: UserData(
                        playbackPositionTicks = event.positionTicks,
                        played = percent >= 0.95,
                        lastPlayedDate = event.datePlayed
                    )
                )
                current[index] = updated
                _items.value = current
            }
        }
        sortRequests.tryEmit(Unit)
    }

    private suspend fun removeIfCompleted(event: PlaybackEvent.Stop) {
        mutex.withLock {
            val current = _items.value.toMutableList()
            val index = current.indexOfFirst { it.id == event.itemId }
            if (index != -1) {
                val item = current[index]
                val runtime = item.runTimeTicks ?: event.runTimeTicks
                val percent = if (runtime > 0) event.positionTicks.toDouble() / runtime else 0.0
                if (percent >= 0.95) {
                    current.removeAt(index)
                    _items.value = current
                }
            }
        }
        sortRequests.tryEmit(Unit)
    }

    private suspend fun sortList() {
        mutex.withLock {
            _items.value = _items.value.sortedByDescending { it.userData?.lastPlayedDate }
        }
    }
}
