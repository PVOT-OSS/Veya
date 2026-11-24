// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys =
        [
            ForeignKey(
                entity = ConversationEntity::class,
                parentColumns = ["id"],
                childColumns = ["conversationId"],
                onDelete = ForeignKey.CASCADE,
            )
        ],
    indices = [Index("conversationId", "createdAt")],
)
internal data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val conversationId: String,
    val kind: String,
    val text: String,
    val intentId: String?,
    val createdAt: Long,
)
