// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol

import android.os.Parcel
import android.os.Parcelable

class ContextTypeParcel(
    @JvmField val id: String,
    @JvmField val label: String,
    @JvmField val synonyms: Array<String>,
    @JvmField val flagKeys: Array<String>,
    @JvmField val flagSynonyms: Array<Array<String>>,
) : Parcelable {
    init {
        require(flagKeys.size == flagSynonyms.size) { "Each flag needs its synonyms" }
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        ParcelFrame.write(dest) {
            dest.writeString(id)
            dest.writeString(label)
            dest.writeStringArray(synonyms)
            dest.writeInt(flagKeys.size)
            for (i in flagKeys.indices) {
                dest.writeString(flagKeys[i])
                dest.writeStringArray(flagSynonyms[i])
            }
        }
    }

    companion object {
        @JvmField
        val CREATOR =
            object : Parcelable.Creator<ContextTypeParcel> {
                override fun createFromParcel(source: Parcel): ContextTypeParcel =
                    ParcelFrame.read(source) {
                        val id = source.readString().orEmpty()
                        val label = source.readString().orEmpty()
                        val synonyms = source.createStringArray() ?: emptyArray()
                        val count = source.readInt().coerceIn(0, Protocol.MAX_FLAGS)
                        val keys = Array(count) { "" }
                        val words = Array(count) { emptyArray<String>() }
                        for (i in 0 until count) {
                            keys[i] = source.readString().orEmpty()
                            words[i] = source.createStringArray() ?: emptyArray()
                        }
                        ContextTypeParcel(id, label, synonyms, keys, words)
                    }

                override fun newArray(size: Int): Array<ContextTypeParcel?> = arrayOfNulls(size)
            }
    }
}
