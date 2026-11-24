// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol

import android.os.Parcel
import android.os.Parcelable

class ContextItemParcel(
    @JvmField val id: String,
    @JvmField val type: String,
    @JvmField val title: String,
    @JvmField val content: String,
    @JvmField val timestamp: Long,
    @JvmField val expiresAt: Long,
    @JvmField val sensitivity: Int,
    @JvmField val metadataKeys: Array<String>,
    @JvmField val metadataValues: Array<String>,
) : Parcelable {
    init {
        require(metadataKeys.size == metadataValues.size) {
            "Metadata keys and values must pair up"
        }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        ParcelFrame.write(dest) {
            dest.writeString(id)
            dest.writeString(type)
            dest.writeString(title)
            dest.writeString(content)
            dest.writeLong(timestamp)
            dest.writeLong(expiresAt)
            dest.writeInt(sensitivity)
            dest.writeStringArray(metadataKeys)
            dest.writeStringArray(metadataValues)
        }
    }

    companion object {
        @JvmField
        val CREATOR =
            object : Parcelable.Creator<ContextItemParcel> {
                override fun createFromParcel(source: Parcel): ContextItemParcel =
                    ParcelFrame.read(source) {
                        ContextItemParcel(
                            id = source.readString().orEmpty(),
                            type = source.readString().orEmpty(),
                            title = source.readString().orEmpty(),
                            content = source.readString().orEmpty(),
                            timestamp = source.readLong(),
                            expiresAt = source.readLong(),
                            sensitivity = source.readInt(),
                            metadataKeys = source.createStringArray() ?: EMPTY,
                            metadataValues = source.createStringArray() ?: EMPTY,
                        )
                    }

                override fun newArray(size: Int): Array<ContextItemParcel?> = arrayOfNulls(size)
            }

        private val EMPTY = emptyArray<String>()
    }
}
