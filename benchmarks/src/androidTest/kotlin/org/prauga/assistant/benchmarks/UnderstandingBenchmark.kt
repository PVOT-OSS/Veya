// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.benchmarks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.capabilities.commands.CapabilityCommands
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.AndroidPlatformGate
import org.prauga.assistant.platform.android.DeviceHandlers
import org.prauga.assistant.understanding.Durations
import org.prauga.assistant.understanding.Lexicon
import org.prauga.assistant.understanding.Times

@RunWith(AndroidJUnit4::class)
class UnderstandingBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val lexicon = Lexicon.english()
    private val times = Times(lexicon)
    private val durations = Durations(lexicon)
    private val now = ZonedDateTime.now()

    @Test
    fun loadLexicon() {
        Latency.measure("lexicon.load", iterations = LOAD_ITERATIONS) {
            Lexicon.load(listOf(Locale.GERMAN, Locale.ENGLISH))
        }
    }

    @Test
    fun buildEngine() {
        Latency.measure("engine.build", iterations = LOAD_ITERATIONS) { engine() }
    }

    @Test
    fun parseTime() {
        Latency.measure("parse.time") { times.parse("next friday at half past seven pm", now) }
    }

    @Test
    fun parseDuration() {
        Latency.measure("parse.duration") { durations.parse("an hour and a half") }
    }

    @Test
    fun resolveCommand() {
        val engine = engine()
        Latency.measure("intent.command") { engine.resolve("Set a timer for 10 minutes") }
    }

    @Test
    fun resolveEvent() {
        val engine = engine()
        Latency.measure("intent.event") { engine.resolve("add dinner with Sam tomorrow at 8pm") }
    }

    @Test
    fun resolveUnmatched() {
        val engine = engine()
        Latency.measure("intent.unmatchedAllCommands") {
            engine.resolve("why do batteries wear out over the years and what can I do")
        }
    }

    private fun engine() =
        IntentEngine(
            DeviceHandlers.create(context, lexicon) + CapabilityCommands.create(context, lexicon),
            AndroidPlatformGate(context),
            Utterance.Fillers.of(lexicon),
        )

    private companion object {
        const val LOAD_ITERATIONS = 200
    }
}
