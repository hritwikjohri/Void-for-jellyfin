package com.hritwik.avoid.data.repository

import com.hritwik.avoid.domain.model.library.MediaItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock


class NextUpStore {
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private var initialLimit: Int = 0
    private var isValid: Boolean = false

    suspend fun setInitial(list: List<MediaItem>, limit: Int) {
        mutex.withLock {
            if (!isValid) {
                _items.value = list
                initialLimit = limit
                isValid = true
            }
        }
    }

    suspend fun update(list: List<MediaItem>) {
        mutex.withLock {
            _items.value = list
            isValid = true
        }
    }

    fun snapshot(limit: Int): List<MediaItem> {
        return _items.value.take(limit)
    }

    fun canServe(limit: Int): Boolean {
        return isValid && _items.value.size >= limit
    }

    suspend fun invalidate() {
        mutex.withLock {
            isValid = false
        }
    }
}
