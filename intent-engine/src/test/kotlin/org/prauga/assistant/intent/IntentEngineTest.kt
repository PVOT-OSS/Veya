// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.assistant.intent.PatternMatcher.Companion.rule
import org.prauga.assistant.understanding.Lexicon

class IntentEngineTest {
    private val battery =
        FakeHandler("battery", rule("(what's )?(my )?battery( level| percentage)?", 0.95f))
    private val torchOn = FakeHandler("torch.on", rule("turn on (the )?(flashlight|torch)", 0.95f))
    private val openApp =
        FakeHandler(
            "open.app",
            rule("(open|launch|start) (?<app>.+)", 0.8f, "app"),
            parameters = listOf(IntentParameter("app", required = true)),
        )

    @Test
    fun matchesTheBestHandler() {
        val engine = IntentEngine(listOf(battery, torchOn, openApp), FakeGate())
        val resolution = engine.resolve("What's my battery percentage?")
        assertEquals("battery", (resolution as Resolution.Matched).candidate.match.intentId)
    }

    @Test
    fun extractsEntities() {
        val engine = IntentEngine(listOf(openApp), FakeGate())
        val resolution = engine.resolve("Open Camera") as Resolution.Matched
        assertEquals("camera", resolution.candidate.match.entities["app"])
    }

    @Test
    fun returnsNoMatchForOpenQuestions() {
        val engine = IntentEngine(listOf(battery, torchOn, openApp), FakeGate())
        assertEquals(Resolution.NoMatch, engine.resolve("why is the sky blue"))
        assertEquals(Resolution.NoMatch, engine.resolve("   "))
    }

    @Test
    fun ignoresMatchesBelowThreshold() {
        val weak = FakeHandler("weak", rule("weather", 0.4f))
        assertEquals(Resolution.NoMatch, IntentEngine(listOf(weak), FakeGate()).resolve("weather"))
    }

    @Test
    fun reportsAmbiguityBetweenCloseMatches() {
        val a = FakeHandler("settings.wifi", rule("open wifi", 0.9f))
        val b = FakeHandler("open.app", rule("open (?<app>.+)", 0.85f, "app"))
        val resolution = IntentEngine(listOf(a, b), FakeGate()).resolve("open wifi")
        assertTrue(resolution is Resolution.Ambiguous)
        assertEquals(
            listOf("settings.wifi", "open.app"),
            (resolution as Resolution.Ambiguous).candidates.map { it.match.intentId },
        )
    }

    @Test
    fun clearWinnerIsNotAmbiguous() {
        val a = FakeHandler("settings.wifi", rule("open wifi( settings)?", 0.95f))
        val b = FakeHandler("open.app", rule("open (?<app>.+)", 0.7f, "app"))
        val resolution = IntentEngine(listOf(a, b), FakeGate()).resolve("open wifi settings")
        assertEquals("settings.wifi", (resolution as Resolution.Matched).candidate.match.intentId)
    }

    @Test
    fun requiresMissingPermissions() {
        val calendar =
            FakeHandler(
                "calendar.next",
                rule("next meeting", 0.9f),
                permissions = setOf("READ_CALENDAR"),
            )
        val denied = IntentEngine(listOf(calendar), FakeGate()).resolve("next meeting")
        assertEquals(setOf("READ_CALENDAR"), (denied as Resolution.NeedsPermission).permissions)
        val granted =
            IntentEngine(listOf(calendar), FakeGate(granted = setOf("READ_CALENDAR")))
                .resolve("next meeting")
        assertTrue(granted is Resolution.Matched)
    }

    @Test
    fun reportsMissingRequiredParameters() {
        val handler =
            FakeHandler(
                "call",
                rule("call( (?<who>.+))?", 0.9f, "who"),
                parameters = listOf(IntentParameter("who", required = true)),
            )
        val resolution = IntentEngine(listOf(handler), FakeGate()).resolve("call")
        assertEquals("who", (resolution as Resolution.MissingParameter).parameter.name)
    }

    @Test
    fun excludesHandlersOutsideTheirSdkRange() {
        val newOnly = FakeHandler("new", rule("hello", 0.9f), minSdk = 40)
        val engine = IntentEngine(listOf(newOnly), FakeGate(sdkInt = 36))
        assertEquals(Resolution.NoMatch, engine.resolve("hello"))
        assertTrue(engine.registered.isEmpty())
    }

    @Test
    fun resolvesAChosenIntentDirectly() {
        val engine = IntentEngine(listOf(battery, openApp), FakeGate())
        val resolution = engine.resolve("open.app", mapOf("app" to "maps")) as Resolution.Matched
        assertEquals("maps", resolution.candidate.match.entities["app"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsDuplicateIds() {
        IntentEngine(listOf(battery, battery), FakeGate())
    }

    @Test
    fun suggestsTheClosestPhrasesIncludingOneTypo() {
        val phrases =
            Lexicon.parse(
                """
                [intent alarm]
                0.95 (set|create)( an)? alarm( for {time})?
                [intent torch]
                0.95 (turn|switch) on (the )?(flashlight|torch)
                """
                    .trimIndent()
            )
        val alarm = MatcherHandler("alarm", PatternMatcher.from("alarm", phrases.phrases("alarm")))
        val torch = MatcherHandler("torch", PatternMatcher.from("torch", phrases.phrases("torch")))
        val engine = IntentEngine(listOf(alarm, torch), FakeGate())
        assertEquals(Resolution.NoMatch, engine.resolve("can u set alrm"))
        assertEquals(listOf("alarm"), engine.suggest("can u set alrm").map { it.match.intentId })
        assertEquals(
            listOf("torch"),
            engine.suggest("turn on the lights").map { it.match.intentId },
        )
        assertEquals(emptyList<Candidate>(), engine.suggest("tell me a joke"))
    }
}

private class MatcherHandler(id: String, private val matcher: PatternMatcher) : IntentHandler {
    override val descriptor = IntentDescriptor(id, id)

    override fun match(utterance: Utterance) = matcher.match(utterance)

    override fun closeness(utterance: Utterance) = matcher.closeness(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome =
        IntentOutcome.Reply(match.intentId)
}
