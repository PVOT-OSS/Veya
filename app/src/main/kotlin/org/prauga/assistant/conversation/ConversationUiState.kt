// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.conversation

import org.prauga.assistant.core.IntentChoice

data class ConversationUiState(
    val conversationId: String? = null,
    val entries: List<ConversationEntry> = emptyList(),
    val prompt: ConversationPrompt? = null,
    val busy: Boolean = false,
    val lastReplyLatencyMillis: Long? = null,
)

sealed interface ConversationEntry {
    val key: Long

    data class User(override val key: Long, val text: String) : ConversationEntry

    data class Reply(override val key: Long, val text: String) : ConversationEntry

    data class Failure(override val key: Long, val text: String) : ConversationEntry

    data class Unanswered(override val key: Long) : ConversationEntry
}

sealed interface ConversationPrompt {
    data class Permission(val choice: IntentChoice, val permissions: Set<String>) :
        ConversationPrompt

    data class Clarify(val choices: List<IntentChoice>) : ConversationPrompt

    data class MissingDetail(
        val choice: IntentChoice,
        val parameter: String,
        val question: String?,
    ) : ConversationPrompt

    data object PermissionDenied : ConversationPrompt
}
