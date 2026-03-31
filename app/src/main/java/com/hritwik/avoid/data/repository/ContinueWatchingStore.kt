package com.hritwik.avoid.data.repository

import com.hritwik.avoid.domain.model.library.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class ContinueWatchingStore {
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    suspend fun setInitial(list: List<MediaItem>) {
        mutex.withLock {
            if (_items.value.isEmpty()) {
                _items.value = list.sortedByDescending { it.userData?.lastPlayedDate }
            }
        }
    }

    suspend fun update(list: List<MediaItem>) {
        mutex.withLock {
            // Sort by last played date descending
            _items.value = list.sortedByDescending { it.userData?.lastPlayedDate }
        }
    }
}
