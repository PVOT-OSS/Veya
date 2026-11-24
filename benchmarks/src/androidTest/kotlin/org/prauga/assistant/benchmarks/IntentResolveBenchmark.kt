// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.benchmarks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.platform.android.AndroidPlatformGate
import org.prauga.assistant.platform.android.DeviceHandlers

@RunWith(AndroidJUnit4::class)
class IntentResolveBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val engine = IntentEngine(DeviceHandlers.create(context), AndroidPlatformGate(context))

    @Test
    fun matchedCommand() {
        Latency.measure("intent.matched") { engine.resolve("What's my battery percentage?") }
    }

    @Test
    fun matchedCommandWithEntity() {
        Latency.measure("intent.matchedWithEntity") { engine.resolve("open wifi settings") }
    }

    @Test
    fun unmatchedQuestion() {
        Latency.measure("intent.unmatched") {
            engine.resolve("why do batteries wear out over the years and what can I do")
        }
    }
}
