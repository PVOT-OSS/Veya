// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.core.MessageKind
import org.prauga.assistant.core.StoredMessage
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RoomConversationStoreTest {
    private var next = 0
    private val store =
        RoomConversationStore.inMemory(ApplicationProvider.getApplicationContext()) { "c${++next}" }

    @After
    fun close() {
        store.close()
    }

    @Test
    fun persistsMessagesInOrder() = runTest {
        val id = store.startConversation("battery", 10)
        store.append(id, StoredMessage(MessageKind.USER, "battery", 11))
        store.append(id, StoredMessage(MessageKind.REPLY, "80%", 12, "device.battery"))
        val messages = store.messages(id).first()
        assertEquals(listOf("battery", "80%"), messages.map { it.text })
        assertEquals("device.battery", messages[1].intentId)
        assertEquals(MessageKind.REPLY, messages[1].kind)
    }

    @Test
    fun listsMostRecentlyUpdatedConversationFirst() = runTest {
        val older = store.startConversation("older", 1)
        val newer = store.startConversation("newer", 2)
        store.append(older, StoredMessage(MessageKind.USER, "again", 5))
        assertEquals(listOf(older, newer), store.conversations().first().map { it.id })
        assertEquals(5L, store.conversations().first().first().updatedAt)
    }

    @Test
    fun deletingAConversationRemovesItsMessages() = runTest {
        val id = store.startConversation("gone", 1)
        store.append(id, StoredMessage(MessageKind.USER, "hi", 2))
        store.delete(id)
        assertTrue(store.conversations().first().isEmpty())
        assertTrue(store.messages(id).first().isEmpty())
    }
}
