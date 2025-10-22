package com.hritwik.avoid.data.remote.websocket

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.milliseconds


class ExponentialBackoff(
    private val base: Duration = 1.seconds,
    private val max: Duration = 30.seconds,
    private val random: Random = Random.Default
) {
    private var attempts = 0

    fun nextDelay(): Duration {
        val exp = (base.inWholeMilliseconds * (1L shl attempts)).coerceAtMost(max.inWholeMilliseconds)
        attempts = (attempts + 1).coerceAtMost(30)
        val jitter = random.nextLong(0, base.inWholeMilliseconds)
        return (exp + jitter).milliseconds
    }

    fun reset() { attempts = 0 }
}
