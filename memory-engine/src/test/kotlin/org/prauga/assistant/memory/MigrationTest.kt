// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            MemoryDatabase::class.java,
        )

    @Test
    fun conversationsSurviveTheMoveToVersionTwo() {
        helper.createDatabase(NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO conversations (id, title, createdAt, updatedAt) VALUES ('c1', 'battery', 1, 2)"
            )
            db.execSQL(
                "INSERT INTO messages (conversationId, kind, text, intentId, createdAt) VALUES ('c1', 'USER', 'battery', NULL, 1)"
            )
        }
        helper.runMigrationsAndValidate(NAME, 2, true).use { db ->
            db.query("SELECT COUNT(*) FROM messages WHERE conversationId = 'c1'").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM donated_items").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
