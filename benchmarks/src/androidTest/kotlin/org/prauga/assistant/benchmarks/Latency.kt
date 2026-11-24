// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.benchmarks

import android.os.Bundle
import android.os.Debug
import androidx.test.platform.app.InstrumentationRegistry

object Latency {
    const val WARM_UP_NANOS = 2_000_000_000L
    const val ITERATIONS = 20_000
    private const val NANOS_PER_MICRO = 1_000.0

    inline fun measure(name: String, crossinline operation: () -> Unit) {
        val warmUpEnd = System.nanoTime() + WARM_UP_NANOS
        while (System.nanoTime() < warmUpEnd) operation()
        val samples = LongArray(ITERATIONS)
        val bytesBefore = allocatedBytes()
        for (i in samples.indices) {
            val start = System.nanoTime()
            operation()
            samples[i] = System.nanoTime() - start
        }
        val bytesPerOp = (allocatedBytes() - bytesBefore) / samples.size
        report(name, samples, bytesPerOp)
    }

    fun allocatedBytes(): Long = Debug.getRuntimeStat("art.gc.bytes-allocated").toLong()

    fun report(name: String, samples: LongArray, bytesPerOp: Long) {
        samples.sort()
        fun micros(fraction: Double): String =
            "%.1f".format(samples[((samples.size - 1) * fraction).toInt()] / NANOS_PER_MICRO)
        val line =
            "$name p50=${micros(0.50)}us p90=${micros(0.90)}us p99=${micros(0.99)}us " +
                "max=${micros(1.0)}us bytesPerOp=$bytesPerOp n=${samples.size}"
        InstrumentationRegistry.getInstrumentation()
            .sendStatus(0, Bundle().apply { putString("latency", line) })
    }
}
