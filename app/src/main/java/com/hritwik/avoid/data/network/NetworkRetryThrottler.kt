package com.hritwik.avoid.data.network

import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicLong


object NetworkRetryThrottler {
    private const val RETRY_WINDOW_MS = 5_000L
    private val lastFailureTime = AtomicLong(0L)

    suspend fun throttleIfNeeded() {
        val now = System.currentTimeMillis()
        val waitTime = RETRY_WINDOW_MS - (now - lastFailureTime.get())
        if (waitTime > 0) {
            delay(waitTime)
        }
    }

    fun onFailure() {
        lastFailureTime.set(System.currentTimeMillis())
    }
}
