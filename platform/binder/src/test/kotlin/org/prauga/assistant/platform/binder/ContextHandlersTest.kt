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
import org.prauga.assistant.context.ContextFlagInfo
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextProvider
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.ContextSource
import org.prauga.assistant.context.ContextTypeInfo
import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PlatformGate
import org.prauga.assistant.intent.Resolution
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ContextHandlersTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val engine = ContextEngine({ ContextGrant(true, Sensitivity.SENSITIVE) })
    private var enabled = true
    private val readiness =
        object : ContextReadiness {
            override fun anyEnabled() = enabled

            override suspend fun ensureConnected(timeoutMillis: Long) = Unit
        }
    private val intents =
        IntentEngine(
            ContextHandlers.create(context, engine, readiness),
            object : PlatformGate {
                override val sdkInt = 35

                override fun isGranted(permission: String) = true
            },
        )
    private val queries = mutableListOf<ContextQuery>()

    private val unread = ContextFlagInfo("unread", listOf("unread", "new"))
    private val messageType =
        ContextTypeInfo(
            "message",
            "Messages",
            listOf("messages", "texts", "message"),
            listOf(unread),
        )
    private val conversationType =
        ContextTypeInfo(
            "conversation",
            "Conversations",
            listOf("chats", "conversations"),
            listOf(unread),
        )
    private val noteType = ContextTypeInfo("note", "Notes", listOf("notes", "memos"))

    private fun app(
        packageName: String,
        label: String,
        vocabulary: List<ContextTypeInfo>,
        items: List<ContextItem>,
    ) =
        object : ContextProvider {
            override val descriptor =
                ContextProviderDescriptor(
                    "live:$packageName",
                    packageName,
                    label,
                    vocabulary.mapTo(HashSet()) { it.id },
                    Sensitivity.SENSITIVE,
                    vocabulary,
                )

            override suspend fun query(query: ContextQuery): List<ContextItem> {
                queries += query
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

    private val messagesSource = ContextSource("org.prauga.messages", "Messages")
    private val notesSource = ContextSource("org.prauga.notes", "Notes")

    private fun message(id: String, who: String, text: String, isUnread: Boolean) =
        ContextItem(
            id,
            "message",
            who,
            text,
            messagesSource,
            10L,
            sensitivity = Sensitivity.SENSITIVE,
            metadata = mapOf("unread" to isUnread.toString()),
        )

    private fun registerApps() {
        engine.register(
            app(
                "org.prauga.messages",
                "Messages",
                listOf(messageType, conversationType),
                listOf(
                    message("m1", "Sam", "Dinner at 8?", true),
                    message("m2", "Alex", "Package arrives", false),
                ),
            )
        )
        engine.register(
            app(
                "org.prauga.notes",
                "Notes",
                listOf(noteType),
                listOf(
                    ContextItem(
                        "n1",
                        "note",
                        "Groceries",
                        "milk, eggs",
                        notesSource,
                        5L,
                        sensitivity = Sensitivity.PERSONAL,
                    )
                ),
            )
        )
    }

    private fun resolve(text: String) = (intents.resolve(text) as? Resolution.Matched)?.candidate

    private fun reply(text: String): String = runBlocking {
        val candidate = checkNotNull(resolve(text)) { "No match for \"$text\"" }
        (candidate.handler.execute(candidate.match) as IntentOutcome.Reply).text
    }

    @Test
    fun understandsWordsTheAppsDeclared() {
        registerApps()
        assertTrue(reply("any unread texts").contains("Sam, unread: Dinner at 8? (Messages)"))
        assertEquals(setOf("message"), queries.last().types)
        assertTrue(!reply("show me my new texts").contains("Alex"))
        reply("notes about groceries")
        assertEquals("groceries", queries.last().text)
        assertEquals(setOf("note"), queries.last().types)
        assertTrue(reply("my memos").contains("Groceries: milk, eggs (Notes)"))
    }

    @Test
    fun understandsPeopleAndApps() {
        registerApps()
        reply("texts from Sam")
        assertEquals("sam", queries.last().text)
        reply("what did Sam say")
        assertEquals(setOf("message"), queries.last().types)
        assertEquals("sam", queries.last().text)
        assertTrue(
            reply("unread texts in Notes")
                .startsWith(context.getString(R.string.context_nothing_of, "unread texts"))
        )
    }

    @Test
    fun aDeclaredTypeWinsOverPlainSearch() {
        registerApps()
        assertEquals(VocabularyHandler.ID, resolve("search texts for dinner")?.match?.intentId)
        assertEquals(ContextSearchHandler.ID, resolve("search for dinner")?.match?.intentId)
        assertEquals(ContextSearchHandler.ID, resolve("search spotify for jazz")?.match?.intentId)
    }

    @Test
    fun leavesOpenQuestionsAlone() {
        registerApps()
        assertEquals(Resolution.NoMatch, intents.resolve("why do messages fail to send"))
        assertEquals(Resolution.NoMatch, intents.resolve("show my recipes"))
    }

    @Test
    fun learnsNewAppsWithoutAnUpdate() {
        assertEquals(Resolution.NoMatch, intents.resolve("show my recordings"))
        engine.register(
            app(
                "org.prauga.recorder",
                "Recorder",
                listOf(
                    ContextTypeInfo("recording", "Recordings", listOf("recordings", "voice notes"))
                ),
                listOf(
                    ContextItem(
                        "r1",
                        "recording",
                        "Standup",
                        "",
                        ContextSource("org.prauga.recorder", "Recorder"),
                        1L,
                        sensitivity = Sensitivity.PERSONAL,
                    )
                ),
            )
        )
        assertTrue(reply("show my recordings").contains("Standup (Recorder)"))
        assertTrue(reply("voice notes about standup").contains("Standup"))
    }

    @Test
    fun plainSearchStillWorksAcrossApps() {
        registerApps()
        assertTrue(reply("search for milk").contains("Groceries: milk, eggs (Notes)"))
        assertEquals(
            context.getString(R.string.context_nothing_found, "zebra"),
            reply("search for zebra"),
        )
    }

    @Test
    fun separatesNothingEnabledFromNotAnswering() {
        enabled = false
        assertEquals(context.getString(R.string.context_nothing_enabled), reply("search for milk"))
        enabled = true
        assertEquals(context.getString(R.string.context_unreachable), reply("search for milk"))
    }
}
