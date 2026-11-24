// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index

@Entity(
    tableName = "donated_items",
    primaryKeys = ["packageName", "itemId"],
    indices = [Index("packageName", "timestamp"), Index("expiresAt")],
)
internal data class DonatedItemEntity(
    val packageName: String,
    val itemId: String,
    val type: String,
    val title: String,
    val content: String,
    val sourceLabel: String,
    val timestamp: Long,
    val expiresAt: Long?,
    val sensitivity: Int,
    val metadata: String,
)

@Fts4(contentEntity = DonatedItemEntity::class)
@Entity(tableName = "donated_items_fts")
internal data class DonatedItemFts(val title: String, val content: String)
