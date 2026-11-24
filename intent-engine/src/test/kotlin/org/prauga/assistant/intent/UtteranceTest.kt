// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import org.junit.Assert.assertEquals
import org.junit.Test

class UtteranceTest {
    @Test
    fun normalizesCaseFillersAndPunctuation() {
        assertEquals(
            "turn on the flashlight",
            Utterance("Hey Veya, please turn on the FLASHLIGHT!").normalized,
        )
    }

    @Test
    fun collapsesWhitespace() {
        assertEquals("open settings", Utterance("  open    settings  ").normalized)
    }

    @Test
    fun keepsInnerApostrophes() {
        assertEquals("what's my battery", Utterance("What\u2019s my battery?").normalized)
    }
}
