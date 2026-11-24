// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

data class LaunchableApp(val label: String, val packageName: String)

object AppMatcher {
    /**
     * The app [query] names best. With [wholeWords], the query must be the whole label or whole
     * words of it, so "sam" never names "Samsung Calendar".
     */
    fun best(
        query: String,
        apps: List<LaunchableApp>,
        wholeWords: Boolean = false,
    ): LaunchableApp? {
        val wanted = simplify(query)
        if (wanted.isEmpty()) return null
        val scored =
            apps.mapNotNull { app ->
                val label = simplify(app.label)
                if (wholeWords && " $label ".indexOf(" $wanted ") < 0) null
                else score(wanted, label)?.let { app to it }
            }
        return scored
            .maxWithOrNull(
                compareBy<Pair<LaunchableApp, Int>> { it.second }.thenBy { -it.first.label.length }
            )
            ?.first
    }

    private fun score(wanted: String, label: String): Int? =
        when {
            label == wanted -> EXACT
            label.startsWith(wanted) -> PREFIX
            label.split(' ').any { it == wanted } -> WORD
            label.contains(wanted) -> CONTAINS
            else -> null
        }

    private fun simplify(text: String): String =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N} ]"), "").replace(Regex("\\s+"), " ").trim()

    private const val EXACT = 4
    private const val PREFIX = 3
    private const val WORD = 2
    private const val CONTAINS = 1
}
