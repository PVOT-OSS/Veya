// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.Duration

object Durations {
    private val UNITS =
        mapOf(
            "second" to 1L,
            "seconds" to 1L,
            "sec" to 1L,
            "secs" to 1L,
            "s" to 1L,
            "minute" to 60L,
            "minutes" to 60L,
            "min" to 60L,
            "mins" to 60L,
            "m" to 60L,
            "hour" to 3_600L,
            "hours" to 3_600L,
            "hr" to 3_600L,
            "hrs" to 3_600L,
            "h" to 3_600L,
            "day" to 86_400L,
            "days" to 86_400L,
            "d" to 86_400L,
        )
    private val COMPACT = Regex("(\\d+(?:\\.\\d+)?)([a-z]+)")
    private val ARTICLES = setOf("a", "an")
    private val QUARTER_SKIP = setOf("of", "an", "a")
    private val AFTER_ARTICLE = setOf("quarter", "half", "couple")
    private const val HALF = 0.5
    private const val QUARTER = 0.25
    private const val MILLIS = 1_000.0

    fun parse(text: String): Duration? {
        val tokens = expand(Tokens.of(text))
        var seconds = 0.0
        var lastUnit = 0L
        var index = 0
        var parts = 0
        while (index < tokens.size) {
            val word = tokens[index]
            when {
                word == "and" || word == "for" -> index++
                word in ARTICLES && tokens.getOrNull(index + 1) in AFTER_ARTICLE && parts == 0 ->
                    index++
                word == "a" && tokens.getOrNull(index + 1) == "half" && parts > 0 -> {
                    seconds += lastUnit * HALF
                    index += 2
                }
                word == "quarter" ||
                    (word == "half" && tokens.getOrNull(index + 1) in ARTICLES) -> {
                    var next = index + 1
                    while (next < tokens.size && tokens[next] in QUARTER_SKIP) next++
                    val unit = UNITS[tokens.getOrNull(next)] ?: return null
                    seconds += unit * if (word == "quarter") QUARTER else HALF
                    lastUnit = unit
                    index = next + 1
                    parts++
                }
                else -> {
                    val number = Numbers.read(tokens, index) ?: return null
                    val unit = UNITS[tokens.getOrNull(index + number.consumed)] ?: return null
                    seconds += number.value * unit
                    lastUnit = unit
                    index += number.consumed + 1
                    parts++
                }
            }
        }
        if (parts == 0 || seconds <= 0) return null
        return Duration.ofMillis((seconds * MILLIS).toLong())
    }

    private fun expand(tokens: List<String>): List<String> =
        tokens.flatMap { token ->
            val match = COMPACT.matchEntire(token)
            if (match != null && match.groupValues[2] in UNITS)
                listOf(match.groupValues[1], match.groupValues[2])
            else listOf(token)
        }
}
