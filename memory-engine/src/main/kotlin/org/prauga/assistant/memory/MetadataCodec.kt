// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import org.json.JSONException
import org.json.JSONObject

internal object MetadataCodec {
    fun encode(metadata: Map<String, String>): String =
        if (metadata.isEmpty()) "" else JSONObject(metadata).toString()

    fun decode(text: String): Map<String, String> {
        if (text.isEmpty()) return emptyMap()
        return try {
            val json = JSONObject(text)
            buildMap(json.length()) { json.keys().forEach { key -> put(key, json.optString(key)) } }
        } catch (e: JSONException) {
            emptyMap()
        }
    }
}
