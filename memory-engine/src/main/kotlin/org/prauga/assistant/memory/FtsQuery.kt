// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

internal object FtsQuery {
    private const val MAX_TERMS = 8

    fun from(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val terms = ArrayList<String>(MAX_TERMS)
        val current = StringBuilder()
        fun flush() {
            if (current.length > 1 && terms.size < MAX_TERMS) terms += current.toString() + "*"
            current.setLength(0)
        }
        for (c in text) {
            if (c.isLetterOrDigit()) current.append(c.lowercaseChar()) else flush()
        }
        flush()
        return if (terms.isEmpty()) null else terms.joinToString(" OR ")
    }
}
