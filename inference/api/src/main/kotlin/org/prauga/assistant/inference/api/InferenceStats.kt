// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.api

data class InferenceStats(
    val timeToFirstChunkMillis: Long?,
    val totalMillis: Long,
    val chunks: Int,
    val outputTokens: Int?,
    val setupMillis: Long? = null,
    val promptTokens: Int? = null,
    val prefillTokensPerSecond: Double? = null,
) {
    val decodeTokensPerSecond: Double?
        get() {
            val tokens = outputTokens ?: return null
            val first = timeToFirstChunkMillis ?: return null
            val decodeMillis = totalMillis - first
            if (tokens <= 1 || decodeMillis <= 0) return null
            return (tokens - 1) * MILLIS_PER_SECOND / decodeMillis
        }

    private companion object {
        const val MILLIS_PER_SECOND = 1_000.0
    }
}

class InferenceTimer(private val nanoTime: () -> Long = System::nanoTime) {
    private val start = nanoTime()
    private var firstChunk: Long? = null
    private var chunks = 0

    fun onChunk() {
        if (firstChunk == null) firstChunk = nanoTime()
        chunks++
    }

    fun finish(outputTokens: Int?): InferenceStats =
        InferenceStats(
            timeToFirstChunkMillis = firstChunk?.let { (it - start) / NANOS_PER_MILLI },
            totalMillis = (nanoTime() - start) / NANOS_PER_MILLI,
            chunks = chunks,
            outputTokens = outputTokens,
        )

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
