// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.context

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContextEngineTest {
    private var now = 1_000L
    private val granted = mutableMapOf<String, ContextGrant>()
    private val engine = ContextEngine({ granted[it.id] ?: ContextGrant.Denied }, clock = { now })

    private class FakeProvider(
        id: String,
        packageName: String = "com.example.$id",
        types: Set<String> = setOf("note"),
        maxSensitivity: Sensitivity = Sensitivity.SENSITIVE,
        private val delayMillis: Long = 0,
        private val failure: Boolean = false,
        private val items: () -> List<ContextItem>,
    ) : ContextProvider {
        var calls = 0
        override val descriptor =
            ContextProviderDescriptor(id, packageName, id, types, maxSensitivity)

        override suspend fun query(query: ContextQuery): List<ContextItem> {
            calls++
            if (delayMillis > 0) delay(delayMillis)
            if (failure) throw ContextProviderException("broken")
            return items()
        }
    }

    private fun item(
        id: String,
        title: String = id,
        content: String = "",
        packageName: String = "com.example.notes",
        type: String = "note",
        timestamp: Long = 500,
        sensitivity: Sensitivity = Sensitivity.PERSONAL,
        expiresAt: Long? = null,
    ) =
        ContextItem(
            id,
            type,
            title,
            content,
            ContextSource(packageName, "Notes"),
            timestamp,
            expiresAt,
            sensitivity,
        )

    private fun allow(id: String, max: Sensitivity = Sensitivity.PERSONAL) {
        granted[id] = ContextGrant(true, max)
    }

    @Test
    fun providersAreOffUntilTheUserGrantsThem() = runTest {
        engine.register(FakeProvider("notes") { listOf(item("a")) })
        val result = engine.query(ContextQuery())
        assertTrue(result.items.isEmpty())
        assertEquals(listOf(SkippedProvider("notes", SkipReason.NOT_GRANTED)), result.skipped)
    }

    @Test
    fun returnsGrantedItemsRankedByRelevanceThenRecency() = runTest {
        allow("notes")
        engine.register(
            FakeProvider("notes") {
                listOf(
                    item("old", title = "Groceries", content = "milk eggs", timestamp = 100),
                    item("new", title = "Trip", content = "pack milk", timestamp = 900),
                    item("other", title = "Ideas", timestamp = 950),
                )
            }
        )
        val result = engine.query(ContextQuery(text = "milk groceries"))
        assertEquals(listOf("old", "new", "other"), result.items.map { it.id })
    }

    @Test
    fun enforcesTheSensitivityCeiling() = runTest {
        allow("notes", Sensitivity.PERSONAL)
        engine.register(
            FakeProvider("notes") {
                listOf(
                    item("ok"),
                    item("secret", sensitivity = Sensitivity.SENSITIVE),
                    item("pub", sensitivity = Sensitivity.PUBLIC),
                )
            }
        )
        assertEquals(setOf("ok", "pub"), engine.query(ContextQuery()).items.map { it.id }.toSet())
    }

    @Test
    fun providerCannotRaiseItsOwnDeclaredCeiling() = runTest {
        allow("notes", Sensitivity.SENSITIVE)
        engine.register(
            FakeProvider("notes", maxSensitivity = Sensitivity.PUBLIC) {
                listOf(item("personal"), item("pub", sensitivity = Sensitivity.PUBLIC))
            }
        )
        assertEquals(listOf("pub"), engine.query(ContextQuery()).items.map { it.id })
    }

    @Test
    fun dropsItemsClaimingToComeFromAnotherApp() = runTest {
        allow("notes")
        engine.register(
            FakeProvider("notes", packageName = "com.example.notes") {
                listOf(item("mine"), item("spoofed", packageName = "com.bank.app"))
            }
        )
        assertEquals(listOf("mine"), engine.query(ContextQuery()).items.map { it.id })
    }

    @Test
    fun dropsExpiredItemsAndOldOnesWhenAsked() = runTest {
        allow("notes")
        engine.register(
            FakeProvider("notes") {
                listOf(
                    item("expired", expiresAt = 999),
                    item("valid", expiresAt = 2_000, timestamp = 800),
                    item("old", timestamp = 10),
                )
            }
        )
        assertEquals(
            setOf("valid", "old"),
            engine.query(ContextQuery()).items.map { it.id }.toSet(),
        )
        assertEquals(listOf("valid"), engine.query(ContextQuery(since = 500)).items.map { it.id })
    }

    @Test
    fun filtersByType() = runTest {
        allow("files")
        engine.register(
            FakeProvider(
                "files",
                packageName = "com.example.files",
                types = setOf("file", "folder"),
            ) {
                listOf(
                    item("doc", type = "file", packageName = "com.example.files"),
                    item("dir", type = "folder", packageName = "com.example.files"),
                )
            }
        )
        assertEquals(
            listOf("doc"),
            engine.query(ContextQuery(types = setOf("file"))).items.map { it.id },
        )
        val skipped = engine.query(ContextQuery(types = setOf("call"))).skipped
        assertEquals(listOf(SkippedProvider("files", SkipReason.TYPE_NOT_OFFERED)), skipped)
    }

    @Test
    fun slowAndFailingProvidersAreSkippedWithoutBlockingOthers() = runTest {
        allow("slow")
        allow("broken")
        allow("notes")
        engine.register(FakeProvider("slow", delayMillis = 5_000) { listOf(item("late")) })
        engine.register(FakeProvider("broken", failure = true) { emptyList() })
        engine.register(
            FakeProvider("notes", packageName = "com.example.notes") { listOf(item("fast")) }
        )
        val result = engine.query(ContextQuery())
        assertEquals(listOf("fast"), result.items.map { it.id })
        assertEquals(
            setOf(
                SkippedProvider("slow", SkipReason.TIMED_OUT),
                SkippedProvider("broken", SkipReason.FAILED),
            ),
            result.skipped.toSet(),
        )
    }

    @Test
    fun capsAndTrimsWhatProvidersReturn() = runTest {
        allow("notes")
        engine.register(
            FakeProvider("notes") {
                List(100) { item("n$it", title = "  title $it  ", content = "x".repeat(5_000)) }
            }
        )
        val items = engine.query(ContextQuery(limit = 50)).items
        assertEquals(ContextEngine.DEFAULT_MAX_ITEMS_PER_PROVIDER, items.size)
        assertTrue(
            items.all {
                it.content.length == ContextEngine.DEFAULT_MAX_CONTENT_CHARS &&
                    !it.title.startsWith(" ")
            }
        )
    }

    @Test
    fun cachesBrieflyAndInvalidatesOnRequest() = runTest {
        allow("notes")
        val provider = FakeProvider("notes") { listOf(item("a")) }
        engine.register(provider)
        engine.query(ContextQuery())
        engine.query(ContextQuery())
        assertEquals(1, provider.calls)
        engine.invalidate("notes")
        engine.query(ContextQuery())
        assertEquals(2, provider.calls)
        now += 60_000
        engine.query(ContextQuery())
        assertEquals(3, provider.calls)
    }

    @Test
    fun unregisteredProvidersAreNotAsked() = runTest {
        allow("notes")
        val provider = FakeProvider("notes") { listOf(item("a")) }
        engine.register(provider)
        engine.unregister("notes")
        assertTrue(engine.query(ContextQuery()).items.isEmpty())
        assertEquals(0, provider.calls)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnboundedLimits() {
        ContextQuery(limit = 500)
    }
}
