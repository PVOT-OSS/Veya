// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

/**
 * Reads a phrase template for the text any match must contain: literal runs, `(a|b)` groups and
 * `<list>`s that are not optional. Only the top level is read, and a top-level `|` means nothing is
 * required, so the result never rejects text the pattern would match.
 */
internal object Requirements {
    private const val QUANTIFIERS = "?*+{"
    private const val SPECIAL = "()[]{}<>\\.*+?^$|"

    fun of(template: String, words: (String) -> Collection<String>): List<List<String>> {
        if (hasTopLevelAlternation(template)) return emptyList()
        val required = ArrayList<List<String>>()
        val run = StringBuilder()
        fun flush() {
            val literal = run.toString().trim()
            if (literal.isNotEmpty()) required += listOf(literal)
            run.setLength(0)
        }
        var index = 0
        while (index < template.length) {
            val char = template[index]
            when {
                char == '\\' -> {
                    flush()
                    index += 2
                }
                char == '(' -> {
                    flush()
                    val end = closing(template, index, '(', ')')
                    if (end < 0) return required
                    if (!optional(template, end + 1)) {
                        alternatives(template.substring(index + 1, end))?.let { required += it }
                    }
                    index = end + 1
                }
                char == '[' -> {
                    flush()
                    index =
                        closing(template, index, '[', ']').takeIf { it >= 0 }?.plus(1)
                            ?: return required
                }
                char == '<' && Lexicon.listEnd(template, index) > 0 -> {
                    flush()
                    val end = Lexicon.listEnd(template, index)
                    if (!optional(template, end + 1)) {
                        required += words(template.substring(index + 1, end)).toList()
                    }
                    index = end + 1
                }
                char == '{' -> {
                    val slot = Lexicon.slotEnd(template, index)
                    if (slot < 0 && run.isNotEmpty()) run.setLength(run.length - 1)
                    flush()
                    index =
                        (if (slot > 0) slot else template.indexOf('}', index))
                            .takeIf { it >= 0 }
                            ?.plus(1) ?: return required
                }
                char == '?' || char == '*' || char == '+' -> {
                    if (run.isNotEmpty()) run.setLength(run.length - 1)
                    flush()
                    index++
                }
                char == '.' || char == '^' || char == '$' -> {
                    flush()
                    index++
                }
                else -> {
                    run.append(char)
                    index++
                }
            }
        }
        flush()
        return required
    }

    private fun optional(template: String, at: Int): Boolean =
        at < template.length && template[at] in QUANTIFIERS

    /** The words of a plain `a|b|c` group, or null when it holds anything else. */
    private fun alternatives(group: String): List<String>? {
        val body = group.removePrefix("?:")
        if (body.any { it in SPECIAL && it != '|' }) return null
        val options = body.split('|').map { it.trim() }
        return options.takeIf { list -> list.none { it.isEmpty() } }
    }

    private fun closing(template: String, start: Int, open: Char, close: Char): Int {
        var depth = 0
        var index = start
        while (index < template.length) {
            when (template[index]) {
                '\\' -> index++
                open -> depth++
                close -> if (--depth == 0) return index
            }
            index++
        }
        return -1
    }

    private fun hasTopLevelAlternation(template: String): Boolean {
        var depth = 0
        var index = 0
        while (index < template.length) {
            when (template[index]) {
                '\\' -> index++
                '(',
                '[',
                '{' -> depth++
                ')',
                ']',
                '}' -> depth--
                '|' -> if (depth == 0) return true
            }
            index++
        }
        return false
    }
}
