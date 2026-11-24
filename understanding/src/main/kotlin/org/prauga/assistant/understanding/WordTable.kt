// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

/** Words or multi-word phrases of one language, each with an optional value. */
class WordTable(entries: Map<String, Double>) {
    class Hit(val value: Double, val consumed: Int)

    private val byLength: Array<Map<String, Double>>

    val keys: Set<String> = entries.keys

    init {
        val longest = entries.keys.maxOfOrNull { it.split(' ').size } ?: 0
        byLength =
            Array(longest + 1) { length -> entries.filterKeys { it.split(' ').size == length } }
    }

    operator fun contains(word: String?): Boolean =
        word != null && byLength.getOrNull(1)?.containsKey(word) == true

    operator fun get(word: String?): Double? = word?.let { byLength.getOrNull(1)?.get(it) }

    /** The longest entry starting at [from], or null. */
    fun at(tokens: List<String>, from: Int): Hit? {
        for (length in minOf(byLength.size - 1, tokens.size - from) downTo 1) {
            val key =
                if (length == 1) tokens[from]
                else tokens.subList(from, from + length).joinToString(" ")
            byLength[length][key]?.let {
                return Hit(it, length)
            }
        }
        return null
    }

    companion object {
        val EMPTY = WordTable(emptyMap())
    }
}
