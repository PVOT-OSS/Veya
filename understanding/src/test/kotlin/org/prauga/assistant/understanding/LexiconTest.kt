// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LexiconTest {
    @Test
    fun expandsListsAndSlots() {
        val lexicon =
            Lexicon.parse(
                """
                [words light]
                torch | flash light
                [intent torch.on]
                0.9 turn on (the )?<light>
                0.8 open {app}( please)?
                0.7 call {number=\+?\d[\d ]+\d}
                """
                    .trimIndent()
            )
        val (light, open, call) = lexicon.phrases("torch.on")
        assertTrue(light.pattern.matches("turn on the flash light"))
        assertEquals(0.9f, light.confidence)
        assertTrue(light.slots.isEmpty())
        assertEquals(
            "maps",
            open.pattern.matchEntire("open maps please")?.groups?.get("app")?.value,
        )
        assertEquals(listOf("number"), call.slots)
        assertNull(call.pattern.matchEntire("call sam"))
    }

    @Test
    fun runtimeListsWinAndEmptyListsNeverMatch() {
        val lexicon = Lexicon.parse("[intent x]\n0.9 show {type=<types>}( \\p{L}+)?")
        val phrase = lexicon.phrases("x", mapOf("types" to listOf("notes"))).single()
        assertTrue(phrase.pattern.matches("show notes"))
        assertTrue(phrase.pattern.matches("show notes today"))
        val none = lexicon.phrases("x", mapOf("types" to emptyList())).single()
        assertFalse(none.pattern.matches("show notes"))
    }

    @Test
    fun aNewLanguageIsOnlyData() {
        val french =
            Lexicon.parse(
                """
                [values number]
                une = 1
                deux = 2
                [values number.tens]
                vingt = 20
                [values duration.unit]
                minutes = 60
                heure = 3600
                heures = 3600
                [words duration.joiner]
                et
                [values duration.fraction]
                demi = 0.5
                [words duration.fraction.link]
                de
                [words duration.and.half]
                demie
                """
                    .trimIndent()
            )
        val durations = Durations(french)
        assertEquals(Duration.ofMinutes(90), durations.parse("une heure et demie"))
        assertEquals(Duration.ofMinutes(20), durations.parse("vingt minutes"))
        assertNull(durations.parse("twenty minutes"))
    }

    @Test
    fun weekdayNamesComeFromThePlatform() {
        val lexicon = Lexicon.load(listOf(Locale.GERMAN))
        val now = ZonedDateTime.of(2026, 11, 24, 10, 15, 0, 0, ZoneOffset.UTC)
        val times = Times(lexicon)
        assertEquals(27, times.parse("freitag 8am", now)?.at?.dayOfMonth)
        assertEquals(27, times.parse("friday 8am", now)?.at?.dayOfMonth)
    }

    @Test
    fun englishIsAlwaysLoaded() {
        val lexicon = Lexicon.load(listOf(Locale.forLanguageTag("xx")))
        assertEquals(listOf("en"), lexicon.languages)
        assertNotNull(lexicon.phrases("app.open").firstOrNull())
    }
}
