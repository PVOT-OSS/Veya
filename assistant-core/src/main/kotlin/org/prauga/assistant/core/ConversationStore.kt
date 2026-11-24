// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.core

import kotlinx.coroutines.flow.Flow

interface ConversationStore {
    suspend fun startConversation(title: String, createdAt: Long): String

    suspend fun append(conversationId: String, message: StoredMessage)

    fun conversations(): Flow<List<ConversationSummary>>

    fun messages(conversationId: String): Flow<List<StoredMessage>>

    suspend fun delete(conversationId: String)
}

data class ConversationSummary(val id: String, val title: String, val updatedAt: Long)

data class StoredMessage(
    val kind: MessageKind,
    val text: String,
    val createdAt: Long,
    val intentId: String? = null,
    val id: Long = 0,
)

enum class MessageKind {
    USER,
    REPLY,
    FAILURE,
    UNANSWERED,
}
