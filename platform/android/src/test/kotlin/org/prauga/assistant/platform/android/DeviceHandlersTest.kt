// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PlatformGate
import org.prauga.assistant.intent.Resolution
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
    private val engine =
        IntentEngine(DeviceHandlers.create(ApplicationProvider.getApplicationContext()), gate)

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
}
