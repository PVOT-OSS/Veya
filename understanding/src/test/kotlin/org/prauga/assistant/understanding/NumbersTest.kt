// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumbersTest {
    private val numbers = Numbers(Lexicon.english())

    @Test
    fun readsDigitsAndWords() {
        val cases =
            mapOf(
                "7" to 7.0,
                "1.5" to 1.5,
                "one" to 1.0,
                "twelve" to 12.0,
                "nineteen" to 19.0,
                "twenty" to 20.0,
                "twenty five" to 25.0,
                "twenty-five" to 25.0,
                "ninety nine" to 99.0,
                "a" to 1.0,
                "an" to 1.0,
                "half" to 0.5,
                "couple" to 2.0,
                "two hundred" to 200.0,
                "a hundred and five" to 105.0,
                "two thousand three hundred and five" to 2305.0,
                "five hundred thousand" to 500_000.0,
                "3 million" to 3_000_000.0,
                "twenty five thousand" to 25_000.0,
            )
        cases.forEach { (text, value) ->
            assertEquals(text, value, numbers.parse(text) ?: Double.NaN, 1e-9)
        }
    }

    @Test
    fun rejectsWhatIsNotANumber() {
        listOf("", "seven apples", "lots", "twenty zero", "1.2.3").forEach {
            assertNull(it, numbers.parse(it))
        }
    }
}
