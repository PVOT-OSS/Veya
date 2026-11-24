// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol

import android.os.Parcel
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ParcelTest {
    private val item =
        ContextItemParcel(
            id = "n1",
            type = "note",
            title = "Groceries",
            content = "milk, eggs",
            timestamp = 42L,
            expiresAt = Protocol.NO_EXPIRY,
            sensitivity = Protocol.SENSITIVITY_PERSONAL,
            metadataKeys = arrayOf("folder"),
            metadataValues = arrayOf("inbox"),
        )

    private inline fun <T> roundTrip(write: (Parcel) -> Unit, read: (Parcel) -> T): T {
        val parcel = Parcel.obtain()
        try {
            write(parcel)
            parcel.setDataPosition(0)
            return read(parcel)
        } finally {
            parcel.recycle()
        }
    }

    @Test
    fun itemsSurviveARoundTrip() {
        val copy =
            roundTrip({ item.writeToParcel(it, 0) }, ContextItemParcel.CREATOR::createFromParcel)
        assertEquals("n1", copy.id)
        assertEquals("note", copy.type)
        assertEquals("Groceries", copy.title)
        assertEquals("milk, eggs", copy.content)
        assertEquals(42L, copy.timestamp)
        assertEquals(Protocol.NO_EXPIRY, copy.expiresAt)
        assertEquals(Protocol.SENSITIVITY_PERSONAL, copy.sensitivity)
        assertArrayEquals(arrayOf("folder"), copy.metadataKeys)
        assertArrayEquals(arrayOf("inbox"), copy.metadataValues)
    }

    @Test
    fun arraysOfItemsKeepTheirBoundaries() {
        val other =
            ContextItemParcel(
                "n2",
                "note",
                "Trip",
                "",
                7L,
                99L,
                Protocol.SENSITIVITY_PUBLIC,
                emptyArray(),
                emptyArray(),
            )
        val copies =
            roundTrip(
                { it.writeTypedArray(arrayOf(item, other), 0) },
                { it.createTypedArray(ContextItemParcel.CREATOR) },
            )
        assertEquals(listOf("n1", "n2"), copies?.map { it.id })
        assertEquals(99L, copies?.get(1)?.expiresAt)
    }

    @Test
    fun readersSkipFieldsAddedByNewerVersions() {
        val copies =
            roundTrip(
                { parcel ->
                    ParcelFrame.write(parcel) {
                        parcel.writeString("n9")
                        parcel.writeString("note")
                        parcel.writeString("Title")
                        parcel.writeString("Body")
                        parcel.writeLong(1L)
                        parcel.writeLong(Protocol.NO_EXPIRY)
                        parcel.writeInt(Protocol.SENSITIVITY_PUBLIC)
                        parcel.writeStringArray(emptyArray())
                        parcel.writeStringArray(emptyArray())
                        parcel.writeString("a field from protocol version 2")
                        parcel.writeLong(123L)
                    }
                    item.writeToParcel(parcel, 0)
                },
                {
                    listOf(
                        ContextItemParcel.CREATOR.createFromParcel(it),
                        ContextItemParcel.CREATOR.createFromParcel(it),
                    )
                },
            )
        assertEquals(listOf("n9", "n1"), copies.map { it.id })
    }

    @Test
    fun queriesSurviveARoundTrip() {
        val query = ContextQueryParcel(null, arrayOf("call"), ContextQueryParcel.NO_SINCE, 10)
        val copy =
            roundTrip({ query.writeToParcel(it, 0) }, ContextQueryParcel.CREATOR::createFromParcel)
        assertNull(copy.text)
        assertArrayEquals(arrayOf("call"), copy.types)
        assertEquals(ContextQueryParcel.NO_SINCE, copy.since)
        assertEquals(10, copy.limit)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsFramesLongerThanTheData() {
        roundTrip({ it.writeInt(10_000) }, ContextItemParcel.CREATOR::createFromParcel)
    }
}
