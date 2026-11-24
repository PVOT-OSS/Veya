// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol

import android.os.Parcel
import android.os.Parcelable

class ContextQueryParcel(
    @JvmField val text: String?,
    @JvmField val types: Array<String>,
    @JvmField val since: Long,
    @JvmField val limit: Int,
) : Parcelable {
    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        ParcelFrame.write(dest) {
            dest.writeString(text)
            dest.writeStringArray(types)
            dest.writeLong(since)
            dest.writeInt(limit)
        }
    }

    companion object {
        const val NO_SINCE = Long.MIN_VALUE

        @JvmField
        val CREATOR =
            object : Parcelable.Creator<ContextQueryParcel> {
                override fun createFromParcel(source: Parcel): ContextQueryParcel =
                    ParcelFrame.read(source) {
                        ContextQueryParcel(
                            text = source.readString(),
                            types = source.createStringArray() ?: emptyArray(),
                            since = source.readLong(),
                            limit = source.readInt(),
                        )
                    }

                override fun newArray(size: Int): Array<ContextQueryParcel?> = arrayOfNulls(size)
            }
    }
}
