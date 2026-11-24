// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

/** Words or multi-word phrases of one language, each with an optional value. */
class WordTable(entries: Map<String, Double>) {
    class Hit(val value: Double, val consumed: Int)

    private class Entry(val words: Array<String>, val value: Double)

    private val single: Map<String, Double> = entries.filterKeys { ' ' !in it }

    /** Entries by their first word, longest first, so lookups compare tokens and build nothing. */
    private val byFirst: Map<String, Array<Entry>> =
        entries.entries
            .map { (key, value) -> Entry(key.split(' ').toTypedArray(), value) }
            .groupBy { it.words[0] }
            .mapValues { (_, list) -> list.sortedByDescending { it.words.size }.toTypedArray() }

    val keys: Set<String> = entries.keys

    operator fun contains(word: String?): Boolean = word != null && single.containsKey(word)

    operator fun get(word: String?): Double? = word?.let(single::get)

    /** True when some entry starts with [word]. */
    fun begins(word: String): Boolean = byFirst.containsKey(word)

    /** The longest entry starting at [from], or null. */
    fun at(tokens: List<String>, from: Int): Hit? {
        if (from >= tokens.size) return null
        val candidates = byFirst[tokens[from]] ?: return null
        for (entry in candidates) {
            val words = entry.words
            if (from + words.size > tokens.size) continue
            var same = true
            for (index in 1 until words.size) {
                if (tokens[from + index] != words[index]) {
                    same = false
                    break
                }
            }
            if (same) return Hit(entry.value, words.size)
        }
        return null
    }

    companion object {
        val EMPTY = WordTable(emptyMap())
    }
}
