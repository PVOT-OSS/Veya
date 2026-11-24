// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {
    @Test
    fun refillsOverTimeAndKeepsCallersApart() {
        var now = 0L
        val limiter = RateLimiter(capacity = 2, refillPerSecond = 1, nanoTime = { now })
        assertTrue(limiter.tryAcquire(1))
        assertTrue(limiter.tryAcquire(1))
        assertFalse(limiter.tryAcquire(1))
        assertTrue(limiter.tryAcquire(2))
        now += 1_000_000_000L
        assertTrue(limiter.tryAcquire(1))
        assertFalse(limiter.tryAcquire(1))
    }
}
