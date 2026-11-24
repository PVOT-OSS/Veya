// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

/** Reads numbers written as digits or as words of the lexicon's languages. */
class Numbers(lexicon: Lexicon) {
    private val words = lexicon.table("number")
    private val tens = lexicon.table("number.tens")

    data class Read(val value: Double, val consumed: Int)

    fun read(tokens: List<String>, from: Int): Read? {
        if (from >= tokens.size) return null
        val first = tokens[from]
        if (DECIMAL.matches(first)) return Read(first.toDouble(), 1)
        val hyphen = first.split('-')
        if (hyphen.size == 2) {
            val ten = tens[hyphen[0]]
            val unit = words[hyphen[1]]
            if (ten != null && unit != null && isDigit(unit)) return Read(ten + unit, 1)
        }
        tens[first]?.let { ten ->
            val unit = words[tokens.getOrNull(from + 1)]
            return if (unit != null && isDigit(unit)) Read(ten + unit, 2) else Read(ten, 1)
        }
        return words.at(tokens, from)?.let { Read(it.value, it.consumed) }
    }

    /** True when a number can start with [word]. */
    fun begins(word: String): Boolean =
        word.firstOrNull()?.isDigit() == true ||
            words.begins(word) ||
            tens.begins(word.substringBefore('-'))

    fun parse(text: String): Double? {
        val tokens = Tokens.of(text)
        val read = read(tokens, 0) ?: return null
        return read.value.takeIf { read.consumed == tokens.size }
    }

    private fun isDigit(value: Double): Boolean = value >= 1 && value <= 9 && value % 1.0 == 0.0

    private companion object {
        val DECIMAL = Regex("\\d+(\\.\\d+)?")
    }
}
