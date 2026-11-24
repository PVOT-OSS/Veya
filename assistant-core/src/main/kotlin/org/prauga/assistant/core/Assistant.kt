// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import org.prauga.assistant.intent.Candidate
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.Resolution

class Assistant(
    private val engine: IntentEngine,
    private val store: ConversationStore,
    private val clock: () -> Long = System::currentTimeMillis,
    private val nanoTime: () -> Long = System::nanoTime,
) {
    fun submit(conversationId: String?, text: String): Flow<AssistantEvent> = flow {
        val trimmed = text.trim()
        require(trimmed.isNotEmpty()) { "Nothing to submit" }
        val started = nanoTime()
        val id = conversationId ?: store.startConversation(trimmed.take(TITLE_LENGTH), clock())
        emit(AssistantEvent.TurnStarted(id))
        store.append(id, StoredMessage(MessageKind.USER, trimmed, clock()))
        respond(id, engine.resolve(trimmed), started)
    }

    fun choose(conversationId: String, choice: IntentChoice): Flow<AssistantEvent> = flow {
        val started = nanoTime()
        emit(AssistantEvent.TurnStarted(conversationId))
        respond(conversationId, engine.resolve(choice.intentId, choice.entities), started)
    }

    private suspend fun FlowCollector<AssistantEvent>.respond(
        conversationId: String,
        resolution: Resolution,
        started: Long,
    ) {
        when (resolution) {
            is Resolution.Matched -> execute(conversationId, resolution.candidate, started)
            is Resolution.Ambiguous ->
                emit(AssistantEvent.Clarify(resolution.candidates.map { it.toChoice() }))
            is Resolution.NeedsPermission ->
                emit(
                    AssistantEvent.NeedsPermission(
                        resolution.candidate.toChoice(),
                        resolution.permissions,
                    )
                )
            is Resolution.MissingParameter ->
                emit(
                    AssistantEvent.MissingDetail(
                        resolution.candidate.toChoice(),
                        resolution.parameter,
                    )
                )
            Resolution.NoMatch -> {
                store.append(conversationId, StoredMessage(MessageKind.UNANSWERED, "", clock()))
                emit(AssistantEvent.Unanswered(UnansweredReason.NO_LOCAL_MODEL))
            }
        }
    }

    private suspend fun FlowCollector<AssistantEvent>.execute(
        conversationId: String,
        candidate: Candidate,
        started: Long,
    ) {
        val intentId = candidate.match.intentId
        when (val outcome = candidate.handler.execute(candidate.match)) {
            is IntentOutcome.Reply -> {
                val elapsed = (nanoTime() - started) / NANOS_PER_MILLI
                store.append(
                    conversationId,
                    StoredMessage(MessageKind.REPLY, outcome.text, clock(), intentId),
                )
                emit(AssistantEvent.Reply(outcome.text, intentId, elapsed))
            }
            is IntentOutcome.Failed -> {
                store.append(
                    conversationId,
                    StoredMessage(MessageKind.FAILURE, outcome.text, clock(), intentId),
                )
                emit(AssistantEvent.ActionFailed(outcome.text, intentId))
            }
        }
    }

    private fun Candidate.toChoice() =
        IntentChoice(match.intentId, handler.descriptor.displayName, match.entities)

    private companion object {
        const val TITLE_LENGTH = 60
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
