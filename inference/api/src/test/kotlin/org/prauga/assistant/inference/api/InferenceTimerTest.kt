// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InferenceTimerTest {
    @Test
    fun measuresTimeToFirstChunkAndDecodeRate() {
        val ticks = ArrayDeque(listOf(0L, 200_000_000L, 1_200_000_000L))
        val timer = InferenceTimer { ticks.removeFirst() }
        timer.onChunk()
        timer.onChunk()
        val stats = timer.finish(outputTokens = 11)
        assertEquals(200L, stats.timeToFirstChunkMillis)
        assertEquals(1_200L, stats.totalMillis)
        assertEquals(2, stats.chunks)
        assertEquals(10.0, stats.decodeTokensPerSecond ?: 0.0, 0.0001)
    }

    @Test
    fun noChunksMeansNoTimeToFirstChunk() {
        val ticks = ArrayDeque(listOf(0L, 50_000_000L))
        val stats = InferenceTimer { ticks.removeFirst() }.finish(outputTokens = null)
        assertNull(stats.timeToFirstChunkMillis)
        assertNull(stats.decodeTokensPerSecond)
    }

    @Test
    fun decodeRateNeedsMoreThanOneToken() {
        assertNull(InferenceStats(100, 200, 1, 1).decodeTokensPerSecond)
        assertNull(InferenceStats(100, 100, 3, 3).decodeTokensPerSecond)
    }

    @Test
    fun recognisesModelFiles() {
        assertEquals(ModelFormat.LITERT_LM, ModelFormat.forFileName("Gemma.LiteRTLM"))
        assertNull(ModelFormat.forFileName("model.gguf"))
    }
}
