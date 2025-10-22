package com.hritwik.avoid.websocket

import com.hritwik.avoid.data.remote.websocket.ExponentialBackoff
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ExponentialBackoffTest {
    @Test
    fun `backoff grows exponentially with jitter`() {
        val backoff = ExponentialBackoff(base = 1.seconds, max = 30.seconds, random = Random(0))
        val first = backoff.nextDelay()
        val second = backoff.nextDelay()
        val third = backoff.nextDelay()
        assertTrue(first in 1.seconds..2.seconds)
        assertTrue(second in 2.seconds..3.seconds)
        assertTrue(third in 4.seconds..5.seconds)
    }
}
