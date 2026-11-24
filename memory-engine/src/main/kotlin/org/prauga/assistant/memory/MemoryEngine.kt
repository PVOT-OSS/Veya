// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import android.content.Context
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.core.ConversationStore

class MemoryEngine private constructor(private val database: MemoryDatabase) {
    val conversations: ConversationStore = RoomConversationStore(database)

    fun donatedContext(
        engine: ContextEngine,
        clock: () -> Long = System::currentTimeMillis,
    ): DonatedContextStore = DonatedContextStore(database.donations(), engine, clock)

    internal fun databaseForTests(): MemoryDatabase = database

    fun close() {
        database.close()
    }

    companion object {
        fun create(context: Context): MemoryEngine = MemoryEngine(MemoryDatabase.build(context))

        internal fun inMemory(context: Context): MemoryEngine =
            MemoryEngine(MemoryDatabase.inMemory(context))
    }
}
