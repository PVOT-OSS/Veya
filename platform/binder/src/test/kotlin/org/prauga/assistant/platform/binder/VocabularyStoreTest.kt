// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.context.ContextFlagInfo
import org.prauga.assistant.context.ContextTypeInfo
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VocabularyStoreTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val messages =
        AppVocabulary(
            "org.prauga.messages",
            "Messages",
            listOf(
                ContextTypeInfo(
                    "message",
                    "Messages",
                    listOf("texts", "sms"),
                    listOf(ContextFlagInfo("unread", listOf("unread", "new"))),
                )
            ),
        )

    @Test
    fun survivesARestart() {
        VocabularyStore(context).put(messages)
        assertEquals(mapOf("org.prauga.messages" to messages), VocabularyStore(context).apps.value)
    }

    @Test
    fun forgetsUninstalledApps() {
        val store = VocabularyStore(context)
        store.put(messages)
        store.retainOnly(setOf("org.prauga.notes"))
        assertTrue(store.apps.value.isEmpty())
        assertTrue(VocabularyStore(context).apps.value.isEmpty())
    }
}
