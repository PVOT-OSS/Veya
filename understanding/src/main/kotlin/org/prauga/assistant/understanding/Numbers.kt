// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

/** Reads numbers written as digits or as words of the lexicon's languages. */
class Numbers(lexicon: Lexicon) {
    private val words = lexicon.table("number")
    private val tens = lexicon.table("number.tens")
    private val scales = lexicon.optional("number.scale")
    private val joiners = lexicon.optional("number.and")

    data class Read(val value: Double, val consumed: Int)

    /** A number starting at [from], with scales: "two thousand three hundred and five". */
    fun read(tokens: List<String>, from: Int): Read? {
        val first = readSmall(tokens, from) ?: return null
        var total = 0.0
        var current = first.value
        var index = from + first.consumed
        while (true) {
            val scale = scales[tokens.getOrNull(index)] ?: break
            current *= scale
            index++
            if (scale >= THOUSAND) {
                total += current
                current = 0.0
            }
            if (tokens.getOrNull(index) in scales) continue
            var next = index
            if (tokens.getOrNull(next) in joiners) next++
            val more = readSmall(tokens, next)?.takeIf { it.value >= 1 && it.value % 1.0 == 0.0 }
            if (more == null || tokens[next] !in tens && !isWhole(tokens[next], more)) break
            current += more.value
            index = next + more.consumed
        }
        return Read(total + current, index - from)
    }

    private fun isWhole(token: String, read: Read): Boolean =
        DECIMAL.matches(token) || words[token] == read.value && read.consumed == 1

    private fun readSmall(tokens: List<String>, from: Int): Read? {
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
        const val THOUSAND = 1_000.0
    }
}
