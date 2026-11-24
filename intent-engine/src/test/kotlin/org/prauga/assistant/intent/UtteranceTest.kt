// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.assistant.understanding.Lexicon

class UtteranceTest {
    private val fillers = Utterance.Fillers.of(Lexicon.english())

    @Test
    fun normalizesCaseFillersAndPunctuation() {
        assertEquals(
            "turn on the flashlight",
            Utterance("Hey Veya, please turn on the FLASHLIGHT!", fillers).normalized,
        )
    }

    @Test
    fun withoutFillersEveryWordStays() {
        assertEquals("please open maps", Utterance("Please open Maps").normalized)
    }

    @Test
    fun collapsesWhitespace() {
        assertEquals("open settings", Utterance("  open    settings  ").normalized)
    }

    @Test
    fun keepsInnerApostrophes() {
        assertEquals("what's my battery", Utterance("What\u2019s my battery?").normalized)
    }

    @Test
    fun wakeWordsGoOnlyFromTheStart() {
        assertEquals("call veya test", Utterance("Hey Veya, call Veya Test", fillers).normalized)
        assertEquals("what time is it", Utterance("Veya what time is it", fillers).normalized)
    }
}
