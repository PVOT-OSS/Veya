// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.core

import org.prauga.assistant.intent.IntentParameter

sealed interface AssistantEvent {
    data class TurnStarted(val conversationId: String) : AssistantEvent

    data class Reply(val text: String, val intentId: String, val elapsedMillis: Long) :
        AssistantEvent

    data class ActionFailed(val text: String, val intentId: String) : AssistantEvent

    data class NeedsPermission(val choice: IntentChoice, val permissions: Set<String>) :
        AssistantEvent

    data class Clarify(val choices: List<IntentChoice>) : AssistantEvent

    data class MissingDetail(val choice: IntentChoice, val parameter: IntentParameter) :
        AssistantEvent

    data class Unanswered(val reason: UnansweredReason) : AssistantEvent
}

data class IntentChoice(
    val intentId: String,
    val displayName: String,
    val entities: Map<String, String>,
)

enum class UnansweredReason {
    NO_LOCAL_MODEL
}
