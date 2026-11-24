// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

class RateLimiter(
    private val capacity: Int = DEFAULT_CAPACITY,
    private val refillPerSecond: Int = DEFAULT_REFILL_PER_SECOND,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    private class Bucket(var tokens: Double, var updatedAt: Long)

    private val buckets = HashMap<Int, Bucket>()

    fun tryAcquire(uid: Int): Boolean =
        synchronized(buckets) {
            val now = nanoTime()
            val bucket = buckets.getOrPut(uid) { Bucket(capacity.toDouble(), now) }
            val refilled = (now - bucket.updatedAt) / NANOS_PER_SECOND * refillPerSecond
            bucket.tokens = minOf(capacity.toDouble(), bucket.tokens + refilled)
            bucket.updatedAt = now
            if (bucket.tokens < 1.0) return false
            bucket.tokens -= 1.0
            true
        }

    private companion object {
        const val DEFAULT_CAPACITY = 60
        const val DEFAULT_REFILL_PER_SECOND = 2
        const val NANOS_PER_SECOND = 1_000_000_000.0
    }
}
