// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.core

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.IntentParameter
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.PlatformGate
import org.prauga.assistant.intent.Utterance

class AssistantTest {
    private val store = InMemoryConversationStore()

    private fun handler(
        id: String,
        pattern: String,
        confidence: Float = 0.9f,
        permissions: Set<String> = emptySet(),
        parameters: List<IntentParameter> = emptyList(),
        outcome: (IntentMatch) -> IntentOutcome = { IntentOutcome.Reply("$id ok") },
    ) =
        object : IntentHandler {
            override val descriptor = IntentDescriptor(id, "name of $id", parameters, permissions)
            private val matcher =
                PatternMatcher(
                    id,
                    listOf(
                        PatternMatcher.rule(
                            pattern,
                            confidence,
                            *parameters.map { it.name }.toTypedArray(),
                        )
                    ),
                )

            override fun match(utterance: Utterance) = matcher.match(utterance)

            override suspend fun execute(match: IntentMatch) = outcome(match)
        }

    private fun assistant(
        vararg handlers: IntentHandler,
        granted: Set<String> = emptySet(),
    ): Assistant {
        val gate =
            object : PlatformGate {
                override val sdkInt = 36

                override fun isGranted(permission: String) = permission in granted
            }
        var nanos = 0L
        return Assistant(
            IntentEngine(handlers.toList(), gate),
            store,
            clock = { 1L },
            nanoTime = { nanos.also { nanos += 3_000_000 } },
        )
    }

    @Test
    fun answersDeterministicCommandsAndPersistsTheTurn() = runTest {
        val events = assistant(handler("battery", "battery")).submit(null, "Battery").toList()
        val id = (events[0] as AssistantEvent.TurnStarted).conversationId
        val reply = events[1] as AssistantEvent.Reply
        assertEquals("battery ok", reply.text)
        assertEquals(3L, reply.elapsedMillis)
        assertEquals(listOf(MessageKind.USER, MessageKind.REPLY), store.stored(id).map { it.kind })
        assertEquals("battery", store.stored(id)[1].intentId)
    }

    @Test
    fun continuesAnExistingConversation() = runTest {
        val assistant = assistant(handler("battery", "battery"))
        val first =
            (assistant.submit(null, "battery").toList()[0] as AssistantEvent.TurnStarted)
                .conversationId
        val second =
            (assistant.submit(first, "battery").toList()[0] as AssistantEvent.TurnStarted)
                .conversationId
        assertEquals(first, second)
        assertEquals(4, store.stored(first).size)
    }

    @Test
    fun saysWhenItCannotAnswerWithoutAModel() = runTest {
        val events =
            assistant(handler("battery", "battery")).submit(null, "Write me a poem").toList()
        assertEquals(AssistantEvent.Unanswered(UnansweredReason.NO_LOCAL_MODEL), events[1])
    }

    @Test
    fun asksForPermissionWithoutRunningTheHandler() = runTest {
        var ran = false
        val calendar =
            handler("calendar", "next meeting", permissions = setOf("CAL")) {
                ran = true
                IntentOutcome.Reply("")
            }
        val event =
            assistant(calendar).submit(null, "next meeting").toList()[1]
                as AssistantEvent.NeedsPermission
        assertEquals(setOf("CAL"), event.permissions)
        assertEquals(false, ran)
    }

    @Test
    fun runsAChoiceOnceThePermissionIsGranted() = runTest {
        val calendar = handler("calendar", "next meeting", permissions = setOf("CAL"))
        val choice = IntentChoice("calendar", "name of calendar", emptyMap())
        val events = assistant(calendar, granted = setOf("CAL")).choose("c9", choice).toList()
        assertEquals("calendar ok", (events[1] as AssistantEvent.Reply).text)
    }

    @Test
    fun offersChoicesWhenAmbiguous() = runTest {
        val events =
            assistant(handler("a", "open wifi"), handler("b", "open wifi", 0.88f))
                .submit(null, "open wifi")
                .toList()
        val clarify = events[1] as AssistantEvent.Clarify
        assertEquals(listOf("a", "b"), clarify.choices.map { it.intentId })
    }

    @Test
    fun reportsFailedActions() = runTest {
        val torch = handler("torch", "torch") { IntentOutcome.Failed("no flash") }
        val events = assistant(torch).submit(null, "torch").toList()
        val id = (events[0] as AssistantEvent.TurnStarted).conversationId
        assertTrue(events[1] is AssistantEvent.ActionFailed)
        assertEquals(MessageKind.FAILURE, store.stored(id).last().kind)
    }

    @Test
    fun asksForAMissingDetail() = runTest {
        val open =
            handler("open", "open( (?<app>.+))?", parameters = listOf(IntentParameter("app", true)))
        val event = assistant(open).submit(null, "open").toList()[1] as AssistantEvent.MissingDetail
        assertEquals("app", event.parameter.name)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankInput() = runTest { assistant().submit(null, "   ").toList() }

    @Test
    fun theNextMessageAnswersAMissingDetail() = runTest {
        val timer =
            handler(
                "timer",
                "set a timer( for (?<length>.+))?",
                parameters = listOf(IntentParameter("length", required = true, "How long?")),
            ) {
                IntentOutcome.Reply("timer ${it.entities["length"]}")
            }
        val assistant = assistant(timer, handler("battery", "battery"))
        val asked = assistant.submit(null, "set a timer").toList()
        val id = (asked[0] as AssistantEvent.TurnStarted).conversationId
        val missing = asked[1] as AssistantEvent.MissingDetail
        assertEquals("How long?", missing.parameter.question)
        val answered = assistant.answer(id, missing.choice, "length", "10 minutes").toList()
        assertEquals("timer 10 minutes", (answered[1] as AssistantEvent.Reply).text)
        val changedMind = assistant.answer(id, missing.choice, "length", "battery").toList()
        assertEquals("battery ok", (changedMind[1] as AssistantEvent.Reply).text)
    }
}
