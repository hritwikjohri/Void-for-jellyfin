package com.hritwik.avoid.data.repository

import com.hritwik.avoid.domain.model.library.MediaItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.seconds


class NextUpStore(scope: CoroutineScope) {
    private val mutex = Mutex()
    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private var isDirty = true
    private var lastRequestLimit = 0

    private data class RefreshRequest(
        val limit: Int,
        val fetcher: suspend (Int) -> List<MediaItem>?
    )

    private val refreshRequests = MutableSharedFlow<RefreshRequest>(extraBufferCapacity = 64)

    init {
        scope.launch {
            refreshRequests
                .debounce(1.seconds)
                .collectLatest { request ->
                    val result = try {
                        request.fetcher(request.limit)
                    } catch (_: Exception) {
                        null
                    }
                    if (result != null) {
                        setInternal(result, request.limit)
                    }
                }
        }
    }

    suspend fun setInitial(list: List<MediaItem>, limit: Int) {
        setInternal(list, limit)
    }

    suspend fun update(list: List<MediaItem>, limit: Int) {
        setInternal(list, limit)
    }

    suspend fun invalidate() {
        mutex.withLock {
            isDirty = true
            lastRequestLimit = 0
        }
    }

    suspend fun canServe(limit: Int): Boolean = mutex.withLock { !isDirty && lastRequestLimit >= limit }

    fun snapshot(limit: Int): List<MediaItem> {
        val current = _items.value
        return if (limit < current.size) current.take(limit) else current
    }

    fun requestRefresh(limit: Int, fetcher: suspend (Int) -> List<MediaItem>?) {
        refreshRequests.tryEmit(RefreshRequest(limit, fetcher))
    }

    private suspend fun setInternal(list: List<MediaItem>, limit: Int) {
        mutex.withLock {
            _items.value = list
            isDirty = false
            lastRequestLimit = limit
        }
    }
}
