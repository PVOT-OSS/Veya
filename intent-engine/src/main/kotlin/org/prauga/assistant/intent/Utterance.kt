// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import org.prauga.assistant.understanding.Lexicon

class Utterance(val raw: String, fillers: Fillers = Fillers.NONE) {
    val normalized: String = fillers.normalize(raw)

    /**
     * Words that carry no meaning: [words] anywhere ("please", "can you"), and [wake] words only at
     * the start ("hey veya"), so a contact called Veya keeps the name.
     */
    class Fillers(words: Collection<String>, wake: Collection<String> = emptyList()) {
        private val pattern: Regex? = alternation(words)?.let { Regex("\\b$it\\b[,]?") }
        private val leading: Regex? =
            alternation(wake)?.let { Regex("^[\\p{Punct}\\s]*$it\\b[,]?") }

        internal fun normalize(text: String): String {
            val lower = text.lowercase().replace('’', '\'')
            val awake = leading?.replace(lower, " ") ?: lower
            val withoutFillers = pattern?.replace(awake, " ") ?: awake
            return EDGE_PUNCTUATION.replace(WHITESPACE.replace(withoutFillers, " "), "").trim()
        }

        companion object {
            val NONE = Fillers(emptyList())

            fun of(lexicon: Lexicon) =
                Fillers(lexicon.words("utterance.filler"), lexicon.optional("utterance.wake").keys)

            private fun alternation(words: Collection<String>): String? =
                words
                    .takeIf { it.isNotEmpty() }
                    ?.sortedByDescending { it.length }
                    ?.joinToString("|", "(", ")") { Regex.escape(it) }

            private val WHITESPACE = Regex("\\s+")
            private val EDGE_PUNCTUATION = Regex("^[\\p{Punct}\\s]+|[\\p{Punct}\\s]+$")
        }
    }
}
