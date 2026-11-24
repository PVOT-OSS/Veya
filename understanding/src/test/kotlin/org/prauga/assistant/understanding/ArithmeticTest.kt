// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ArithmeticTest {
    private val arithmetic = Arithmetic(Lexicon.english())

    private fun value(text: String): String? =
        (arithmetic.evaluate(text) as? Arithmetic.Result.Value)?.value?.toPlainString()

    @Test
    fun evaluatesSymbolsExactly() {
        mapOf(
                "2+2" to "4",
                "2 + 3 * 4" to "14",
                "(2 + 3) × 4" to "20",
                "0.1 + 0.2" to "0.3",
                "10 / 4" to "2.5",
                "1 / 3" to "0.3333333333",
                "2^10" to "1024",
                "2 ^ -2" to "0.25",
                "-5 + 3" to "-2",
                "1,000 * 3" to "3000",
                "12 ÷ 4 − 1" to "2",
            )
            .forEach { (text, expected) -> assertEquals(text, expected, value(text)) }
    }

    @Test
    fun understandsWordsAndPercentages() {
        mapOf(
                "18% of 240" to "43.2",
                "15 percent of 80" to "12",
                "240 + 15%" to "276",
                "100 - 20%" to "80",
                "twelve divided by five" to "2.4",
                "seven times six" to "42",
                "square root of 81" to "9",
                "the square root of 2" to "1.4142135624",
                "5 squared" to "25",
                "3 cubed" to "27",
                "2 to the power of 8" to "256",
                "two hundred and fifty plus 50" to "300",
            )
            .forEach { (text, expected) -> assertEquals(text, expected, value(text)) }
    }

    @Test
    fun reportsWhatHasNoAnswer() {
        assertEquals(Arithmetic.Result.DivideByZero, arithmetic.evaluate("5 / 0"))
        assertEquals(Arithmetic.Result.Undefined, arithmetic.evaluate("square root of -4"))
        assertEquals(Arithmetic.Result.Undefined, arithmetic.evaluate("2 ^ 0.5"))
    }

    @Test
    fun countsOperationsSoAPlainNumberIsNotASum() {
        assertEquals(Arithmetic.Result.Value(BigDecimal("42"), 0), arithmetic.evaluate("42"))
        assertEquals(1, (arithmetic.evaluate("40 + 2") as Arithmetic.Result.Value).operations)
    }

    @Test
    fun refusesWhatIsNotArithmetic() {
        listOf("open camera", "battery", "call 555 0100", "2 + ", "(2 + 3", "5 miles in km", "")
            .forEach { assertNull(it, value(it)) }
    }
}
