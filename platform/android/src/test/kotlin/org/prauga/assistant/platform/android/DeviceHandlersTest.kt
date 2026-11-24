// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PlatformGate
import org.prauga.assistant.intent.Resolution
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.handlers.DateMathHandler
import org.prauga.assistant.understanding.Lexicon
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DeviceHandlersTest {
    private val gate =
        object : PlatformGate {
            override val sdkInt = 35

            override fun isGranted(permission: String) = true
        }
    private val lexicon = Lexicon.english()
    private val engine =
        IntentEngine(
            DeviceHandlers.create(ApplicationProvider.getApplicationContext(), lexicon),
            gate,
            Utterance.Fillers.of(lexicon),
        )

    private fun intentOf(text: String): String? =
        (engine.resolve(text) as? Resolution.Matched)?.candidate?.match?.intentId

    @Test
    fun routesCommonCommands() {
        assertEquals("device.battery", intentOf("What's my battery percentage?"))
        assertEquals("device.battery", intentOf("how much battery is left"))
        assertEquals("device.torch.on", intentOf("Turn on the flashlight"))
        assertEquals("device.torch.off", intentOf("torch off"))
        assertEquals("settings.open", intentOf("Open Settings"))
        assertEquals("settings.open", intentOf("open wifi settings"))
        assertEquals("settings.open", intentOf("open battery saver settings"))
        assertEquals("app.open", intentOf("open camera"))
        assertEquals("time.now", intentOf("What time is it?"))
        assertEquals("date.today", intentOf("what's the date today"))
        assertEquals("device.info", intentOf("which android version is this"))
    }

    @Test
    fun leavesOpenQuestionsUnmatched() {
        assertEquals(
            Resolution.NoMatch,
            engine.resolve("why do batteries wear out over the years and what can I do"),
        )
        assertEquals(Resolution.NoMatch, engine.resolve("tell me a joke"))
    }

    @Test
    fun openWithoutAnAppAsksForIt() {
        val resolution = engine.resolve("open")
        assertTrue(resolution is Resolution.MissingParameter)
    }

    @Test
    fun answersDeviceInfoAndTime() = runTest {
        val info = (engine.resolve("device info") as Resolution.Matched).candidate
        assertTrue(
            (info.handler.execute(info.match) as IntentOutcome.Reply).text.contains("Android")
        )
        val time = (engine.resolve("what time is it") as Resolution.Matched).candidate
        assertTrue(
            (time.handler.execute(time.match) as IntentOutcome.Reply).text.startsWith("It's ")
        )
    }

    @Test
    fun answersArithmeticExactly() {
        fun answer(text: String): String {
            val candidate = (engine.resolve(text) as Resolution.Matched).candidate
            return runBlocking {
                (candidate.handler.execute(candidate.match) as IntentOutcome.Reply).text
            }
        }
        assertEquals("18% of 240 = 43.2", answer("What's 18% of 240?"))
        assertEquals("(2 + 3) × 4 = 20", answer("(2 + 3) × 4"))
        assertEquals("0.1 + 0.2 = 0.3", answer("0.1 + 0.2"))
        assertEquals("square root of 81 = 9", answer("what is the square root of 81"))
        assertEquals("1000 * 1000 = 1,000,000", answer("1000 * 1000"))
    }

    @Test
    fun aPlainNumberOrOtherCommandIsNotArithmetic() {
        assertEquals("app.open", intentOf("open camera"))
        assertEquals(Resolution.NoMatch, engine.resolve("42"))
        assertEquals("device.battery", intentOf("battery 50%"))
    }

    @Test
    fun convertsUnits() {
        fun answer(text: String): String {
            val candidate = (engine.resolve(text) as Resolution.Matched).candidate
            return runBlocking {
                (candidate.handler.execute(candidate.match) as IntentOutcome.Reply).text
            }
        }
        assertEquals("5 miles is 8.04672 km.", answer("5 miles in km"))
        assertEquals("212 f is 100 celsius.", answer("Convert 212 F to Celsius"))
        assertEquals("a liter is 4.226753 cups.", answer("how many cups in a liter?"))
        assertEquals("100 km/h is 62.137119 mph.", answer("what's 100 km/h in mph"))
        assertEquals("app.open", intentOf("open maps in organic maps"))
    }

    @Test
    fun routesDateQuestions() {
        assertEquals("date.until", intentOf("how many days until christmas"))
        assertEquals("date.until", intentOf("How long until the 25th of December?"))
        assertEquals("date.which", intentOf("what date is 3 weeks from now"))
        assertEquals("date.which", intentOf("when is halloween"))
        assertEquals("date.today", intentOf("what day is it"))
        assertEquals("date.today", intentOf("what's the date today"))
        assertEquals(Resolution.NoMatch, engine.resolve("when is my dentist appointment"))
    }

    @Test
    fun countsDaysFromAFixedToday() {
        val clock = Clock.fixed(Instant.parse("2026-11-24T10:15:00Z"), ZoneOffset.UTC)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val until = DateMathHandler(context, lexicon, until = true, clock)
        val which = DateMathHandler(context, lexicon, until = false, clock)
        fun reply(handler: DateMathHandler, text: String): String {
            val match =
                requireNotNull(handler.match(Utterance(text, Utterance.Fillers.of(lexicon))))
            return runBlocking { (handler.execute(match) as IntentOutcome.Reply).text }
        }
        assertEquals(
            "christmas is in 31 days, on Friday, December 25, 2026.",
            reply(until, "how many days until christmas"),
        )
        assertEquals("That's Tuesday, December 15, 2026.", reply(which, "what date is in 3 weeks"))
        assertEquals(
            "That was Saturday, November 14, 2026.",
            reply(which, "what day was it 10 days ago"),
        )
    }
}
