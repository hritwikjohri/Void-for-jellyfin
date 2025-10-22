package com.hritwik.avoid.data.common

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RepositoryCache(private val ttlMs: Long = 30_000) {
    private data class CacheEntry(val timestamp: Long, val result: NetworkResult<Any>)

    private val cache = mutableMapOf<String, CacheEntry>()
    private val mutex = Mutex()

    @Suppress("UNCHECKED_CAST")
    suspend fun <T> get(key: String, loader: suspend () -> NetworkResult<T>): NetworkResult<T> {
        val now = System.currentTimeMillis()
        mutex.withLock {
            val entry = cache[key]
            if (entry != null && now - entry.timestamp < ttlMs) {
                return entry.result as NetworkResult<T>
            }
        }
        val result = loader()
        mutex.withLock {
            cache[key] = CacheEntry(now, result as NetworkResult<Any>)
        }
        return result
    }

    suspend fun invalidate(key: String) {
        mutex.withLock {
            cache.remove(key)
        }
    }

}
