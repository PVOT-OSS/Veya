// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

object Numbers {
    private val UNITS =
        mapOf(
            "zero" to 0,
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "six" to 6,
            "seven" to 7,
            "eight" to 8,
            "nine" to 9,
            "ten" to 10,
            "eleven" to 11,
            "twelve" to 12,
            "thirteen" to 13,
            "fourteen" to 14,
            "fifteen" to 15,
            "sixteen" to 16,
            "seventeen" to 17,
            "eighteen" to 18,
            "nineteen" to 19,
        )
    private val TENS =
        mapOf(
            "twenty" to 20,
            "thirty" to 30,
            "forty" to 40,
            "fifty" to 50,
            "sixty" to 60,
            "seventy" to 70,
            "eighty" to 80,
            "ninety" to 90,
        )
    private val DECIMAL = Regex("\\d+(\\.\\d+)?")

    data class Read(val value: Double, val consumed: Int)

    fun read(tokens: List<String>, from: Int): Read? {
        if (from >= tokens.size) return null
        val first = tokens[from]
        if (DECIMAL.matches(first)) return Read(first.toDouble(), 1)
        when (first) {
            "a",
            "an" -> return Read(1.0, 1)
            "half" -> return Read(HALF, 1)
            "couple" -> return Read(2.0, 1)
        }
        if (first == "a" && tokens.getOrNull(from + 1) == "couple") return Read(2.0, 2)
        val hyphen = first.split('-')
        if (hyphen.size == 2) {
            val tens = TENS[hyphen[0]]
            val unit = UNITS[hyphen[1]]
            if (tens != null && unit != null && unit in 1..9)
                return Read((tens + unit).toDouble(), 1)
        }
        UNITS[first]?.let {
            return Read(it.toDouble(), 1)
        }
        val tens = TENS[first] ?: return null
        val next = tokens.getOrNull(from + 1)?.let { UNITS[it] }
        return if (next != null && next in 1..9) Read((tens + next).toDouble(), 2)
        else Read(tens.toDouble(), 1)
    }

    fun parse(text: String): Double? {
        val tokens = Tokens.of(text)
        val read = read(tokens, 0) ?: return null
        return read.value.takeIf { read.consumed == tokens.size }
    }

    private const val HALF = 0.5
}
