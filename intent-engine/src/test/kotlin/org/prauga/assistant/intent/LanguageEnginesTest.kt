// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.prauga.assistant.understanding.Lexicon

class LanguageEnginesTest {
    @Test
    fun rebuildsOnlyWhenTheLanguagesChange() {
        var locales = listOf(Locale.ENGLISH)
        val loaded = mutableListOf<List<Locale>>()
        val engines =
            LanguageEngines(
                locales = { locales },
                load = {
                    loaded += it
                    Lexicon.parse("")
                },
                build = { IntentEngine(emptyList(), FakeGate()) },
            )
        val first = engines()
        assertSame(first, engines())
        locales = listOf(Locale.GERMAN, Locale.ENGLISH)
        val second = engines()
        assertNotSame(first, second)
        assertSame(second, engines())
        assertEquals(listOf(listOf(Locale.ENGLISH), listOf(Locale.GERMAN, Locale.ENGLISH)), loaded)
    }
}
