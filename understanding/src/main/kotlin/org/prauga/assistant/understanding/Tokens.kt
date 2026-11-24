// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

internal object Tokens {
    private val SPLIT = Regex("[\\s,]+")

    fun of(text: String): List<String> =
        text.lowercase().replace('\u2019', '\'').trim().split(SPLIT).filter { it.isNotEmpty() }
}
