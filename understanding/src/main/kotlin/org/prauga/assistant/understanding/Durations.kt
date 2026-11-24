// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.Duration

/** Reads lengths of time such as "an hour and a half", "90 seconds" or "2h 5m". */
class Durations(lexicon: Lexicon, private val numbers: Numbers = Numbers(lexicon)) {
    private val units = lexicon.table("duration.unit")
    private val joiners = lexicon.table("duration.joiner")
    private val fractions = lexicon.table("duration.fraction")
    private val fractionLinks = lexicon.table("duration.fraction.link")
    private val andHalf = lexicon.table("duration.and.half")
    private val compact = Regex("(\\d+(?:\\.\\d+)?)(\\p{L}+)")

    fun parse(text: String): Duration? = parse(expand(Tokens.of(text)))

    internal fun parse(tokens: List<String>): Duration? {
        var seconds = 0.0
        var lastUnit = 0.0
        var index = 0
        var parts = 0
        while (index < tokens.size) {
            val half = if (parts > 0) andHalf.at(tokens, index) else null
            val fraction = if (half == null) fractions.at(tokens, index) else null
            when {
                tokens[index] in joiners -> index++
                half != null -> {
                    seconds += lastUnit * HALF
                    index += half.consumed
                }
                fraction != null -> {
                    var next = index + fraction.consumed
                    while (tokens.getOrNull(next) in fractionLinks) next++
                    val unit = units[tokens.getOrNull(next)] ?: return null
                    seconds += unit * fraction.value
                    lastUnit = unit
                    index = next + 1
                    parts++
                }
                else -> {
                    val number = numbers.read(tokens, index) ?: return null
                    val unit = units[tokens.getOrNull(index + number.consumed)] ?: return null
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

    internal fun expand(tokens: List<String>): List<String> =
        tokens.flatMap { token ->
            val match = compact.matchEntire(token)
            if (match != null && match.groupValues[2] in units)
                listOf(match.groupValues[1], match.groupValues[2])
            else listOf(token)
        }

    private companion object {
        const val HALF = 0.5
        const val MILLIS = 1_000.0
    }
}
