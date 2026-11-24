// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import android.content.Context
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.prauga.assistant.core.ConversationStore
import org.prauga.assistant.core.ConversationSummary
import org.prauga.assistant.core.MessageKind
import org.prauga.assistant.core.StoredMessage

class RoomConversationStore
internal constructor(
    private val database: MemoryDatabase,
    private val newId: () -> String = { UUID.randomUUID().toString() },
) : ConversationStore {
    private val dao = database.conversations()

    override suspend fun startConversation(title: String, createdAt: Long): String {
        val id = newId()
        dao.insert(ConversationEntity(id, title, createdAt, createdAt))
        return id
    }

    override suspend fun append(conversationId: String, message: StoredMessage) {
        dao.append(
            MessageEntity(
                conversationId = conversationId,
                kind = message.kind.name,
                text = message.text,
                intentId = message.intentId,
                createdAt = message.createdAt,
            )
        )
    }

    override fun conversations(): Flow<List<ConversationSummary>> =
        dao.conversations().map { rows ->
            rows.map { ConversationSummary(it.id, it.title, it.updatedAt) }
        }

    override fun messages(conversationId: String): Flow<List<StoredMessage>> =
        dao.messages(conversationId).map { rows ->
            rows.mapNotNull { row ->
                val kind =
                    MessageKind.entries.firstOrNull { it.name == row.kind }
                        ?: return@mapNotNull null
                StoredMessage(kind, row.text, row.createdAt, row.intentId, row.id)
            }
        }

    override suspend fun delete(conversationId: String) {
        dao.delete(conversationId)
    }

    fun close() {
        database.close()
    }

    companion object {
        internal fun inMemory(context: Context, newId: () -> String): RoomConversationStore =
            RoomConversationStore(MemoryDatabase.inMemory(context), newId)
    }
}
