// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextProvider
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.ContextSource
import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PlatformGate
import org.prauga.assistant.intent.Resolution
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContextSearchHandlerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var granted = true
    private var enabled = true
    private val readiness =
        object : ContextReadiness {
            override fun anyEnabled() = enabled

            override suspend fun ensureConnected(timeoutMillis: Long) = Unit
        }
    private val engine =
        ContextEngine({
            if (granted) ContextGrant(true, Sensitivity.SENSITIVE) else ContextGrant.Denied
        })
    private val intents =
        IntentEngine(
            ContextSearchHandler.all(context, engine, readiness),
            object : PlatformGate {
                override val sdkInt = 35

                override fun isGranted(permission: String) = true
            },
        )
    private val source = ContextSource("org.prauga.messages", "Messages")
    var lastQuery: ContextQuery? = null

    private fun register(vararg items: ContextItem) {
        engine.register(
            object : ContextProvider {
                override val descriptor =
                    ContextProviderDescriptor(
                        "live:org.prauga.messages",
                        "org.prauga.messages",
                        "Messages",
                        setOf("conversation", "message"),
                        Sensitivity.SENSITIVE,
                    )

                override suspend fun query(query: ContextQuery): List<ContextItem> {
                    lastQuery = query
                    val words = query.text?.split(' ')?.filter { it.isNotBlank() }.orEmpty()
                    return items.filter { item ->
                        (query.types.isEmpty() || item.type in query.types) &&
                            (words.isEmpty() ||
                                words.any {
                                    item.title.contains(it, true) || item.content.contains(it, true)
                                })
                    }
                }
            }
        )
    }

    private fun conversation(id: String, title: String, unread: Boolean) =
        ContextItem(
            id,
            "conversation",
            title,
            "",
            source,
            timestamp = 1L,
            sensitivity = Sensitivity.PERSONAL,
            metadata = mapOf("unread" to unread.toString()),
        )

    private fun reply(text: String): String = runBlocking {
        val matched = intents.resolve(text) as Resolution.Matched
        (matched.candidate.handler.execute(matched.candidate.match) as IntentOutcome.Reply).text
    }

    @Test
    fun routesContextCommands() {
        fun id(text: String) =
            (intents.resolve(text) as? Resolution.Matched)?.candidate?.match?.intentId
        assertEquals("context.unread", id("Any unread messages?"))
        assertEquals("context.unread", id("unread messages from Sam"))
        assertEquals("context.from", id("messages from Sam"))
        assertEquals("context.from", id("what did Sam say"))
        assertEquals("context.search", id("search messages for dinner"))
        assertEquals("context.search", id("search for dinner"))
    }

    @Test
    fun listsOnlyUnreadConversations() {
        register(conversation("1", "Sam", unread = true), conversation("2", "Alex", unread = false))
        val text = reply("any unread messages")
        assertTrue(text, text.contains("Sam, unread (Messages)"))
        assertTrue(text, !text.contains("Alex"))
        assertEquals(setOf("conversation"), lastQuery?.types)
    }

    @Test
    fun passesThePersonAsTheSearchText() {
        register(conversation("1", "Sam", unread = false))
        reply("messages from Sam")
        assertEquals("sam", lastQuery?.text)
        assertEquals(setOf("conversation", "message"), lastQuery?.types)
    }

    @Test
    fun showsMessageTextWhenTheAppSharesIt() {
        register(
            ContextItem(
                "m1",
                "message",
                "Sam",
                "Dinner at 8?",
                source,
                timestamp = 1L,
                sensitivity = Sensitivity.SENSITIVE,
            )
        )
        val text = reply("search messages for dinner")
        assertTrue(text, text.contains("Sam: Dinner at 8? (Messages)"))
    }

    @Test
    fun saysWhenNothingIsShared() {
        enabled = false
        assertEquals(
            context.getString(R.string.context_nothing_enabled),
            reply("any unread messages"),
        )
    }

    @Test
    fun saysWhenAnEnabledAppIsNotAnswering() {
        assertEquals(context.getString(R.string.context_unreachable), reply("any unread messages"))
    }

    @Test
    fun saysWhenNothingMatches() {
        register(conversation("1", "Sam", unread = false))
        assertEquals(context.getString(R.string.context_nothing_unread), reply("unread messages"))
        assertEquals(
            context.getString(R.string.context_nothing_found, "zebra"),
            reply("search for zebra"),
        )
    }

    @Test
    fun filtersByTheNamedApp() {
        register(conversation("1", "Dinner plans", unread = false))
        assertTrue(
            reply("search notes for dinner")
                .startsWith(context.getString(R.string.context_nothing_found, "dinner"))
        )
    }
}
