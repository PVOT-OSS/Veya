// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol

import android.os.Parcel

internal object ParcelFrame {
    inline fun write(dest: Parcel, body: () -> Unit) {
        val sizePosition = dest.dataPosition()
        dest.writeInt(0)
        val start = dest.dataPosition()
        body()
        val end = dest.dataPosition()
        dest.setDataPosition(sizePosition)
        dest.writeInt(end - start)
        dest.setDataPosition(end)
    }

    inline fun <T> read(source: Parcel, body: (end: Int) -> T): T {
        val size = source.readInt()
        require(size >= 0) { "Negative parcel frame" }
        val start = source.dataPosition()
        val end = start + size
        require(end <= source.dataSize()) { "Parcel frame runs past the data" }
        val result = body(end)
        source.setDataPosition(end)
        return result
    }

    fun hasMore(source: Parcel, end: Int): Boolean = source.dataPosition() < end
}
