// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.sdk.protocol.ContextQueryParcel
import org.prauga.assistant.sdk.protocol.Protocol
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParcelMappingTest {
    @Test
    fun itemsAreCappedToTheProtocolLimits() {
        val parcel =
            ContextItem(
                    id = "i".repeat(500),
                    type = "conversation",
                    title = "t".repeat(500),
                    timestamp = 5L,
                    sensitivity = Sensitivity.SENSITIVE,
                    content = "c".repeat(5_000),
                    metadata = (1..40).associate { "k$it" to "v" },
                )
                .toParcel()
        assertEquals(Protocol.MAX_ID_CHARS, parcel.id.length)
        assertEquals(Protocol.MAX_TITLE_CHARS, parcel.title.length)
        assertEquals(Protocol.MAX_CONTENT_CHARS, parcel.content.length)
        assertEquals(Protocol.MAX_METADATA_ENTRIES, parcel.metadataKeys.size)
        assertEquals(Protocol.SENSITIVITY_SENSITIVE, parcel.sensitivity)
        assertEquals(Protocol.NO_EXPIRY, parcel.expiresAt)
    }

    @Test
    fun queriesClampTheirLimitAndMapSince() {
        val query =
            ContextQueryParcel("sam", arrayOf("conversation"), ContextQueryParcel.NO_SINCE, 100_000)
                .toQuery()
        assertNull(query.since)
        assertEquals(Protocol.MAX_ITEMS_PER_CALL, query.limit)
        assertEquals(setOf("conversation"), query.types)
    }
}
