// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import java.util.Locale
import org.prauga.assistant.understanding.Lexicon

/**
 * The intent engine for the user's current languages. It is built once per language list and
 * rebuilt the first time it is needed after the user changes the phone's languages.
 */
class LanguageEngines(
    private val locales: () -> List<Locale>,
    private val load: (List<Locale>) -> Lexicon = Lexicon::load,
    private val build: (Lexicon) -> IntentEngine,
) : () -> IntentEngine {
    private class Built(val locales: List<Locale>, val engine: IntentEngine)

    @Volatile private var current: Built? = null

    override fun invoke(): IntentEngine {
        val wanted = locales()
        current?.let { if (it.locales == wanted) return it.engine }
        return synchronized(this) {
            current?.takeIf { it.locales == wanted }?.engine
                ?: build(load(wanted)).also { current = Built(wanted, it) }
        }
    }
}
