// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnitsTest {
    private val units = Units(Lexicon.english())

    private fun convert(source: String, target: String) =
        units.convert(source, target)?.toPlainString()

    @Test
    fun convertsWithinAKind() {
        assertEquals("8.04672", convert("5 miles", "km"))
        assertEquals("2.2046226218", convert("1 kg", "pounds"))
        assertEquals("4.2267528377", convert("a liter", "cups"))
        assertEquals("100", convert("212 °f", "celsius"))
        assertEquals("212", convert("100 c", "f"))
        assertEquals("-40", convert("-40 fahrenheit", "celsius"))
        assertEquals("273.15", convert("0 celsius", "kelvin"))
        assertEquals("62.1371192237", convert("100 km/h", "mph"))
        assertEquals("1024", convert("1 mib", "kib"))
        assertEquals("1.5", convert("90 minutes", "hours"))
        assertEquals("2.4710538147", convert("one hectare", "acres"))
        assertEquals("8.04672", convert("5mi", "km"))
        assertEquals("1", convert("100 cm", "m"))
    }

    @Test
    fun refusesKindsThatDoNotMix() {
        assertNull(convert("5 miles", "kg"))
        assertNull(convert("5 apples", "km"))
        assertNull(convert("5 miles", "km please"))
        assertNull(convert("dinner", "my calendar"))
    }
}
