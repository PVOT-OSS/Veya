// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Converts amounts between units of one kind: "5 miles" to "km", "100 °f" to "celsius". Unit words
 * and their sizes come from the lexicon (`unit.<kind>`: size in the kind's base unit); temperatures
 * use `unit.temperature.zero` and `unit.temperature.degree`, in Celsius.
 */
class Units(lexicon: Lexicon, private val numbers: Numbers = Numbers(lexicon)) {
    private class Unit(val kind: String, val size: BigDecimal, val zero: BigDecimal)

    private val tables: List<Pair<String, WordTable>> =
        KINDS.map { it to lexicon.optional("unit.$it") }
    private val temperatureZero = lexicon.optional("unit.temperature.zero")
    private val temperatureDegree = lexicon.optional("unit.temperature.degree")
    private val compact = Regex("(\\d+(?:\\.\\d+)?)([^\\d\\s.].*)")

    /** [source] ("5 miles") in the unit [target] ("km"), or null when they don't convert. */
    fun convert(source: String, target: String): BigDecimal? {
        val targetTokens = Tokens.of(target)
        val sourceTokens = expand(Tokens.of(source))
        if (targetTokens.isEmpty() || sourceTokens.isEmpty()) return null
        val to = units(targetTokens, 0).filter { it.second == targetTokens.size }
        if (to.isEmpty()) return null
        val amount = amount(sourceTokens)
        val start = amount?.consumed ?: 0
        val from = units(sourceTokens, start).filter { it.second == sourceTokens.size }
        for ((fromUnit, _) in from) {
            val toUnit = to.firstOrNull { it.first.kind == fromUnit.kind }?.first ?: continue
            val value = amount?.value ?: BigDecimal.ONE
            val base = value.subtract(fromUnit.zero, MATH).multiply(fromUnit.size, MATH)
            val converted = base.divide(toUnit.size, MATH).add(toUnit.zero, MATH)
            return converted.setScale(SHOWN_DECIMALS, RoundingMode.HALF_EVEN).stripTrailingZeros()
        }
        return null
    }

    private class Amount(val value: BigDecimal, val consumed: Int)

    private fun amount(tokens: List<String>): Amount? {
        tokens[0].toBigDecimalOrNull()?.let {
            return Amount(it, 1)
        }
        val read = numbers.read(tokens, 0) ?: return null
        return Amount(BigDecimal.valueOf(read.value), read.consumed)
    }

    /** Every unit, of any kind, that starts at [from], with the index after it. */
    private fun units(tokens: List<String>, from: Int): List<Pair<Unit, Int>> {
        if (from >= tokens.size) return emptyList()
        val found = ArrayList<Pair<Unit, Int>>(2)
        for ((kind, table) in tables) {
            val hit = table.at(tokens, from) ?: continue
            found +=
                Unit(kind, BigDecimal.valueOf(hit.value), BigDecimal.ZERO) to from + hit.consumed
        }
        val zero = temperatureZero.at(tokens, from)
        val degree = temperatureDegree.at(tokens, from)
        if (zero != null && degree != null && zero.consumed == degree.consumed) {
            found +=
                Unit(
                    TEMPERATURE,
                    BigDecimal.ONE.divide(BigDecimal.valueOf(degree.value), MATH),
                    BigDecimal.valueOf(zero.value),
                ) to from + zero.consumed
        }
        return found
    }

    private fun expand(tokens: List<String>): List<String> =
        tokens.flatMap { token ->
            val match = compact.matchEntire(token)
            if (match != null) listOf(match.groupValues[1], match.groupValues[2]) else listOf(token)
        }

    private companion object {
        val MATH = MathContext.DECIMAL128
        const val SHOWN_DECIMALS = 10
        const val TEMPERATURE = "temperature"
        val KINDS = listOf("length", "mass", "volume", "area", "speed", "data", "time")
    }
}
