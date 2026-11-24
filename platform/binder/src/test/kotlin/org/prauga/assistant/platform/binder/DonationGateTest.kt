// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.memory.Donation
import org.prauga.assistant.memory.DonationRejection
import org.prauga.assistant.memory.DonationResult
import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.Protocol

@OptIn(ExperimentalCoroutinesApi::class)
class DonationGateTest {
    private val enabled = mutableSetOf("com.example.notes")
    private val sink = RecordingSink()
    private val invalidated = mutableListOf<String>()
    private val gate =
        DonationGate(
            packagesForUid = { uid ->
                if (uid == NOTES_UID) arrayOf("com.example.notes") else arrayOf("com.other.app")
            },
            labelFor = { "Label of $it" },
            access =
                object : IntegrationAccess {
                    override fun isEnabled(packageName: String) = packageName in enabled
                },
            limiter = RateLimiter(capacity = 2, refillPerSecond = 0, nanoTime = { 0L }),
            sink = sink,
            invalidate = { invalidated += it },
            scope = TestScope(UnconfinedTestDispatcher()),
        )

    private fun parcel(
        id: String,
        sensitivity: Int = Protocol.SENSITIVITY_PERSONAL,
        expiresAt: Long = Protocol.NO_EXPIRY,
    ) =
        ContextItemParcel(
            id,
            "note",
            "Title $id",
            "Body",
            1L,
            expiresAt,
            sensitivity,
            arrayOf("k"),
            arrayOf("v"),
        )

    private fun donate(uid: Int, packageName: String, vararg items: ContextItemParcel): Int {
        var code = Int.MIN_VALUE
        gate.donate(uid, packageName, items.toList()) { code = it }
        return code
    }

    @Test
    fun storesDonationsFromTheVerifiedCallerWithAResolvedLabel() {
        assertEquals(1, donate(NOTES_UID, "com.example.notes", parcel("1")))
        val call = sink.donations.single()
        assertEquals("com.example.notes", call.first)
        assertEquals("Label of com.example.notes", call.second)
        val donation = call.third.single()
        assertEquals(Sensitivity.PERSONAL, donation.sensitivity)
        assertEquals(null, donation.expiresAt)
        assertEquals(mapOf("k" to "v"), donation.metadata)
    }

    @Test
    fun rejectsCallersClaimingAnotherPackage() {
        assertEquals(
            Protocol.RESULT_CALLER_MISMATCH,
            donate(OTHER_UID, "com.example.notes", parcel("1")),
        )
        var removeCode = 0
        gate.removeAll(OTHER_UID, "com.example.notes") { removeCode = it }
        assertEquals(Protocol.RESULT_CALLER_MISMATCH, removeCode)
        assertTrue(sink.donations.isEmpty())
        assertTrue(sink.removedAll.isEmpty())
    }

    @Test
    fun refusesDonationsUntilTheUserEnablesTheApp() {
        enabled.clear()
        assertEquals(
            Protocol.RESULT_NOT_ENABLED,
            donate(NOTES_UID, "com.example.notes", parcel("1")),
        )
        assertTrue(sink.donations.isEmpty())
    }

    @Test
    fun appsCanAlwaysDeleteTheirOwnData() {
        enabled.clear()
        var code = Int.MIN_VALUE
        gate.remove(NOTES_UID, "com.example.notes", listOf("1", "2")) { code = it }
        assertEquals(2, code)
        gate.removeAll(NOTES_UID, "com.example.notes") { code = it }
        assertEquals(0, code)
        assertEquals(listOf("com.example.notes"), sink.removedAll)
    }

    @Test
    fun capsItemsPerCall() {
        val items = Array(Protocol.MAX_ITEMS_PER_CALL + 1) { parcel("n$it") }
        assertEquals(Protocol.RESULT_TOO_MANY_ITEMS, donate(NOTES_UID, "com.example.notes", *items))
    }

    @Test
    fun rateLimitsEachCaller() {
        assertEquals(1, donate(NOTES_UID, "com.example.notes", parcel("1")))
        assertEquals(1, donate(NOTES_UID, "com.example.notes", parcel("2")))
        assertEquals(
            Protocol.RESULT_RATE_LIMITED,
            donate(NOTES_UID, "com.example.notes", parcel("3")),
        )
    }

    @Test
    fun reportsStoreRejectionsAsProtocolCodes() {
        sink.next = DonationResult.Rejected(DonationRejection.QUOTA_EXCEEDED)
        assertEquals(
            Protocol.RESULT_QUOTA_EXCEEDED,
            donate(NOTES_UID, "com.example.notes", parcel("1")),
        )
    }

    @Test
    fun mapsUnknownSensitivityToTheMostProtectedLevel() {
        donate(NOTES_UID, "com.example.notes", parcel("1", sensitivity = 7, expiresAt = 99L))
        val donation = sink.donations.single().third.single()
        assertEquals(Sensitivity.SENSITIVE, donation.sensitivity)
        assertEquals(99L, donation.expiresAt)
    }

    @Test
    fun onlyTheVerifiedCallerCanInvalidateItsOwnCache() {
        gate.notifyChanged(OTHER_UID, "com.example.notes")
        gate.notifyChanged(NOTES_UID, "com.example.notes")
        assertEquals(listOf("com.example.notes"), invalidated)
    }

    private class RecordingSink : DonationSink {
        val donations = mutableListOf<Triple<String, String, List<Donation>>>()
        val removedAll = mutableListOf<String>()
        var next: DonationResult? = null

        override suspend fun donate(
            packageName: String,
            label: String,
            donations: List<Donation>,
        ): DonationResult {
            this.donations += Triple(packageName, label, donations)
            return next ?: DonationResult.Stored(donations.size)
        }

        override suspend fun remove(packageName: String, itemIds: List<String>) = Unit

        override suspend fun removeAll(packageName: String) {
            removedAll += packageName
        }
    }

    private companion object {
        const val NOTES_UID = 10_123
        const val OTHER_UID = 10_456
    }
}
