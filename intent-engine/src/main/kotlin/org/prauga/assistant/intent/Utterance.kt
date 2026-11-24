// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import org.prauga.assistant.understanding.Lexicon

class Utterance(val raw: String, fillers: Fillers = Fillers.NONE) {
    val normalized: String = fillers.normalize(raw)

    /** Words of the lexicon's languages that carry no meaning ("please", "hey veya"). */
    class Fillers(words: Collection<String>) {
        private val pattern: Regex? =
            words
                .takeIf { it.isNotEmpty() }
                ?.sortedByDescending { it.length }
                ?.let { list ->
                    Regex("\\b(" + list.joinToString("|") { Regex.escape(it) } + ")\\b[,]?")
                }

        internal fun normalize(text: String): String {
            val lower = text.lowercase().replace('’', '\'')
            val withoutFillers = pattern?.replace(lower, " ") ?: lower
            return EDGE_PUNCTUATION.replace(WHITESPACE.replace(withoutFillers, " "), "").trim()
        }

        companion object {
            val NONE = Fillers(emptyList())

            fun of(lexicon: Lexicon) = Fillers(lexicon.words("utterance.filler"))

            private val WHITESPACE = Regex("\\s+")
            private val EDGE_PUNCTUATION = Regex("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$")
        }
    }
}
