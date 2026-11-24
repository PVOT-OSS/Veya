// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ConversationDao {
    @Insert suspend fun insert(conversation: ConversationEntity)

    @Insert suspend fun insert(message: MessageEntity)

    @Query("UPDATE conversations SET updatedAt = :updatedAt WHERE id = :id")
    suspend fun touch(id: String, updatedAt: Long)

    @Transaction
    suspend fun append(message: MessageEntity) {
        insert(message)
        touch(message.conversationId, message.createdAt)
    }

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun conversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt, id")
    fun messages(conversationId: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM conversations WHERE id = :id") suspend fun delete(id: String)
}
