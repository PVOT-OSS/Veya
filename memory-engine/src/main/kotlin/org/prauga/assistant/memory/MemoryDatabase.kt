// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class MemoryDatabase : RoomDatabase() {
    abstract fun conversations(): ConversationDao

    companion object {
        const val NAME = "memory.db"

        fun build(context: Context): MemoryDatabase =
            Room.databaseBuilder(context, MemoryDatabase::class.java, NAME).build()

        fun inMemory(context: Context): MemoryDatabase =
            Room.inMemoryDatabaseBuilder(context, MemoryDatabase::class.java).build()
    }
}
