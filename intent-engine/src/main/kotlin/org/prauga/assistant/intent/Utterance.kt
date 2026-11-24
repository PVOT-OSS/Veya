// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

class Utterance(val raw: String) {
    val normalized: String = normalize(raw)

    private companion object {
        val FILLERS =
            listOf(
                "hey veya",
                "ok veya",
                "veya",
                "please",
                "could you",
                "can you",
                "would you",
                "i want to",
                "i'd like to",
            )
        val WHITESPACE = Regex("\\s+")
        val EDGE_PUNCTUATION = Regex("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$")
        val FILLER_PATTERN =
            Regex("\\b(" + FILLERS.joinToString("|") { Regex.escape(it) } + ")\\b[,]?")

        fun normalize(text: String): String {
            val lower = text.lowercase().replace('\u2019', '\'')
            val withoutFillers = FILLER_PATTERN.replace(lower, " ")
            return EDGE_PUNCTUATION.replace(WHITESPACE.replace(withoutFillers, " "), "").trim()
        }
    }
}
