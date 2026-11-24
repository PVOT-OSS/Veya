// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.Sensitivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DonatedContextStoreTest {
    private var now = 1_000L
    private val granted = mutableSetOf<String>()
    private val engine =
        ContextEngine(
            {
                if (it.packageName in granted) ContextGrant(true, Sensitivity.PERSONAL)
                else ContextGrant.Denied
            },
            clock = { now },
        )
    private val memory = MemoryEngine.inMemory(ApplicationProvider.getApplicationContext())
    private val store = memory.donatedContext(engine) { now }

    @After
    fun close() {
        memory.close()
    }

    private fun note(
        id: String,
        title: String,
        content: String = "",
        type: String = "note",
        timestamp: Long = 500,
        expiresAt: Long? = null,
        sensitivity: Sensitivity = Sensitivity.PERSONAL,
    ) =
        Donation(
            id,
            type,
            title,
            content,
            timestamp,
            sensitivity,
            expiresAt,
            mapOf("folder" to "inbox"),
        )

    @Test
    fun donatedItemsAreSearchableOnceTheAppIsGranted() = runBlocking {
        store.donate(
            "com.example.notes",
            "Notes",
            listOf(note("1", "Groceries", "milk and eggs"), note("2", "Trip", "passport")),
        )
        assertTrue(engine.query(ContextQuery(text = "milk")).items.isEmpty())
        granted += "com.example.notes"
        val items = engine.query(ContextQuery(text = "milk")).items
        assertEquals(listOf("1"), items.map { it.id })
        assertEquals("Notes", items.single().source.label)
        assertEquals(mapOf("folder" to "inbox"), items.single().metadata)
    }

    @Test
    fun matchesWordPrefixesCaseInsensitively() = runBlocking {
        granted += "com.example.notes"
        store.donate(
            "com.example.notes",
            "Notes",
            listOf(note("1", "Quarterly Planning", "budget review")),
        )
        assertEquals(listOf("1"), engine.query(ContextQuery(text = "PLAN")).items.map { it.id })
        assertEquals(listOf("1"), engine.query(ContextQuery(text = "budg?")).items.map { it.id })
        assertTrue(engine.query(ContextQuery(text = "holiday")).items.isEmpty())
    }

    @Test
    fun emptyTextReturnsRecentItemsAndTypesFilter() = runBlocking {
        granted += "com.example.phone"
        store.donate(
            "com.example.phone",
            "Phone",
            listOf(
                note("c1", "Missed call from Sam", type = "call", timestamp = 900),
                note("c2", "Call with Alex", type = "call", timestamp = 950),
                note("v1", "Voicemail from Sam", type = "voicemail", timestamp = 990),
            ),
        )
        assertEquals(listOf("v1", "c2", "c1"), engine.query(ContextQuery()).items.map { it.id })
        assertEquals(
            listOf("c2", "c1"),
            engine.query(ContextQuery(types = setOf("call"))).items.map { it.id },
        )
    }

    @Test
    fun expiredItemsAreHiddenAndPurged() = runBlocking {
        granted += "com.example.notes"
        store.donate(
            "com.example.notes",
            "Notes",
            listOf(note("old", "Boarding pass", expiresAt = 1_500), note("keep", "Boarding info")),
        )
        now = 2_000
        assertEquals(
            listOf("keep"),
            engine.query(ContextQuery(text = "boarding")).items.map { it.id },
        )
        assertEquals(1, store.purgeExpired())
    }

    @Test
    fun updatesInPlaceAndDeletes() = runBlocking {
        granted += "com.example.notes"
        store.donate("com.example.notes", "Notes", listOf(note("1", "Draft")))
        store.donate("com.example.notes", "Notes", listOf(note("1", "Final version")))
        assertEquals(listOf("Final version"), engine.query(ContextQuery()).items.map { it.title })
        store.remove("com.example.notes", listOf("1"))
        assertTrue(engine.query(ContextQuery()).items.isEmpty())
        assertTrue(engine.providers.value.isEmpty())
    }

    @Test
    fun removeAllOnlyAffectsThatApp() = runBlocking {
        granted += setOf("com.example.a", "com.example.b")
        store.donate("com.example.a", "A", listOf(note("1", "alpha")))
        store.donate("com.example.b", "B", listOf(note("1", "beta")))
        store.removeAll("com.example.a")
        assertEquals(
            listOf("com.example.b"),
            engine.query(ContextQuery()).items.map { it.source.packageName },
        )
    }

    @Test
    fun enforcesQuotasAndValidation() = runBlocking {
        val small =
            DonatedContextStore(
                MemoryEngineAccess.dao(memory),
                engine,
                { now },
                maxItemsPerPackage = 2,
            )
        assertEquals(
            DonationResult.Stored(2),
            small.donate("com.example.a", "A", listOf(note("1", "one"), note("2", "two"))),
        )
        assertEquals(
            DonationResult.Stored(1),
            small.donate("com.example.a", "A", listOf(note("2", "two again"))),
        )
        assertEquals(
            DonationResult.Rejected(DonationRejection.QUOTA_EXCEEDED),
            small.donate("com.example.a", "A", listOf(note("3", "three"))),
        )
        assertEquals(
            DonationResult.Rejected(DonationRejection.INVALID_ITEM),
            small.donate("com.example.a", "A", listOf(note(" ", "blank id"))),
        )
        val tooMany = List(DonatedContextStore.MAX_ITEMS_PER_CALL + 1) { note("n$it", "x") }
        assertEquals(
            DonationResult.Rejected(DonationRejection.TOO_MANY_IN_ONE_CALL),
            small.donate("com.example.a", "A", tooMany),
        )
    }

    @Test
    fun capsDonatedText() = runBlocking {
        granted += "com.example.notes"
        store.donate(
            "com.example.notes",
            "Notes",
            listOf(note("1", "t".repeat(1_000), "c".repeat(10_000))),
        )
        val item = engine.query(ContextQuery()).items.single()
        assertEquals(ContextEngine.DEFAULT_MAX_TITLE_CHARS, item.title.length)
        assertEquals(ContextEngine.DEFAULT_MAX_CONTENT_CHARS, item.content.length)
    }

    @Test
    fun ftsQueryIgnoresOperatorsAndPunctuation() {
        assertEquals("milk* OR eggs*", FtsQuery.from("milk, eggs!"))
        assertEquals("near* OR or*", FtsQuery.from("NEAR \"OR\" -* a"))
        assertEquals(null, FtsQuery.from("  ?! "))
    }
}
