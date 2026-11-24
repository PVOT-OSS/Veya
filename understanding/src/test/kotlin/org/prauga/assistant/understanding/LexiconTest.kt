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

    @Test
    fun readsWhatAPhraseCannotMatchWithout() {
        val lexicon =
            Lexicon.parse(
                """
                [words light]
                torch | flashlight
                [intent x]
                0.9 (set|start)( me)?( a| an)? timer( for)? {subject}
                0.9 <light> on
                0.9 count ?down {subject}
                0.9 (open|launch)( {app})?
                0.9 a|b
                0.9 .*\bbattery\b.*
                0.9 call {number=\+?[\d(][\d ()-]*\d}
                """
                    .trimIndent()
            )
        assertEquals(
            listOf(
                listOf(listOf("set", "start"), listOf("timer")),
                listOf(listOf("torch", "flashlight"), listOf("on")),
                listOf(listOf("count"), listOf("down")),
                listOf(listOf("open", "launch")),
                emptyList(),
                listOf(listOf("battery")),
                listOf(listOf("call")),
            ),
            lexicon.phrases("x").map { it.required },
        )
    }

    @Test
    fun requiredWordsNeverRejectAMatch() {
        val lexicon = Lexicon.english()
        val lists = mapOf("types" to listOf("messages", "notes"), "flags" to listOf("unread"))
        val sentences =
            listOf(
                "set a timer for 10 minutes",
                "10 minute timer",
                "countdown 5 minutes",
                "count down from 30 seconds",
                "wake me up at 7am",
                "set an alarm",
                "show my alarms",
                "what alarms do i have",
                "directions to the eiffel tower",
                "navigate",
                "find coffee near me",
                "how do i get to the station",
                "search the web for kotlin",
                "google cats",
                "play some jazz",
                "listen to rain",
                "open example.org",
                "example.org/path",
                "open a website",
                "take a photo",
                "record a video",
                "record a voice note",
                "start recording",
                "add dinner tomorrow at 8pm",
                "add an event",
                "call 555 0100",
                "call",
                "text 555 0100 running late",
                "send a text to 555 0100 hi",
                "turn on the flashlight",
                "torch off",
                "what's my battery",
                "battery",
                "how much battery is left",
                "open camera",
                "open",
                "open wifi settings",
                "settings",
                "what time is it",
                "what's the date today",
                "what day is it",
                "which android version is this",
                "search my notes for milk",
                "search for milk",
                "unread messages",
                "messages from sam",
                "what did sam say",
                "unread",
                "what's 18% of 240",
                "2+2",
                "square root of 81",
            )
        val ids =
            listOf(
                "device.battery",
                "device.torch.on",
                "device.torch.off",
                "device.info",
                "time.now",
                "date.today",
                "app.open",
                "context.search",
                "context.vocabulary",
                "context.vocabulary.messages",
                "command.timer",
                "command.alarm",
                "command.alarms",
                "command.map",
                "command.web",
                "command.play",
                "command.link",
                "command.photo",
                "command.video",
                "command.audio",
                "command.event",
                "command.call",
                "command.text",
                "math.calculate",
            )
        var matched = 0
        ids.forEach { id ->
            lexicon.phrases(id, lists).forEach { phrase ->
                sentences.forEach { text ->
                    if (phrase.pattern.matches(text)) {
                        matched++
                        assertTrue("$id ${phrase.pattern} rejected “$text”", phrase.mayMatch(text))
                    }
                }
            }
        }
        assertTrue(matched > sentences.size)
    }
}
