// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InMemoryConversationStore : ConversationStore {
    private val titles = MutableStateFlow<Map<String, String>>(emptyMap())
    private val messages = MutableStateFlow<Map<String, List<StoredMessage>>>(emptyMap())

    override suspend fun startConversation(title: String, createdAt: Long): String {
        val id = "c${titles.value.size + 1}"
        titles.update { it + (id to title) }
        messages.update { it + (id to emptyList()) }
        return id
    }

    override suspend fun append(conversationId: String, message: StoredMessage) {
        messages.update { it + (conversationId to it[conversationId].orEmpty() + message) }
    }

    override fun conversations(): Flow<List<ConversationSummary>> =
        titles.map { map -> map.map { ConversationSummary(it.key, it.value, 0) } }

    override fun messages(conversationId: String): Flow<List<StoredMessage>> =
        messages.map { it[conversationId].orEmpty() }

    override suspend fun delete(conversationId: String) {
        titles.update { it - conversationId }
        messages.update { it - conversationId }
    }

    fun stored(conversationId: String): List<StoredMessage> =
        messages.value[conversationId].orEmpty()
}
