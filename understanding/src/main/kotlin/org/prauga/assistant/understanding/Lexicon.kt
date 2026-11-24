// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

/** A phrase that means an intent: a compiled pattern, how sure it is, and the slots it fills. */
class Phrase(val pattern: Regex, val confidence: Float, val slots: List<String>)

/**
 * Everything Veya knows about the words of the languages it reads. Code only names meanings
 * ("duration.unit", "app.open"); the words live in `lexicon/<language>.lex` data files, so a new
 * language is a new file, not new code.
 *
 * File format, one entry per line, `#` starts a comment:
 * ```
 * [words name]          one word or phrase per line, or several separated by `|`
 * [values name]         `word = number` per line
 * [intent id]           `confidence pattern` per line
 * ```
 *
 * In patterns, `<name>` is any word of a list and `{slot}` captures text into a slot.
 * `{slot=<name>}` captures only words of that list; `{slot=regex}` captures what the regex matches.
 */
class Lexicon
private constructor(
    val languages: List<String>,
    private val tables: Map<String, Map<String, Double>>,
    private val intents: Map<String, List<Template>>,
) {
    private class Template(val confidence: Float, val pattern: String)

    private val compiled = HashMap<String, WordTable>()

    fun table(name: String): WordTable =
        synchronized(compiled) {
            compiled.getOrPut(name) {
                WordTable(requireNotNull(tables[name]) { "Lexicon has no list $name" })
            }
        }

    fun words(name: String): Set<String> = table(name).keys

    fun has(name: String): Boolean = name in tables

    /** The phrases of [intentId]; `<name>` resolves from [lists] first, then this lexicon. */
    fun phrases(
        intentId: String,
        lists: Map<String, Collection<String>> = emptyMap(),
    ): List<Phrase> =
        intents[intentId].orEmpty().map { template ->
            val slots = ArrayList<String>(2)
            val regex = expand(template.pattern, lists, slots)
            Phrase(Regex(regex), template.confidence, slots)
        }

    private fun expand(
        pattern: String,
        lists: Map<String, Collection<String>>,
        slots: MutableList<String>,
    ): String {
        val out = StringBuilder(pattern.length * 2)
        var index = 0
        while (index < pattern.length) {
            val char = pattern[index]
            val listEnd = if (char == '<') listEnd(pattern, index) else -1
            val slotEnd = if (char == '{') slotEnd(pattern, index) else -1
            when {
                listEnd > 0 -> {
                    out.append(alternation(pattern.substring(index + 1, listEnd), lists))
                    index = listEnd + 1
                }
                slotEnd > 0 -> {
                    val body = pattern.substring(index + 1, slotEnd)
                    val name = body.substringBefore('=')
                    val inner =
                        if ('=' in body) expand(body.substringAfter('='), lists, slots) else ANY
                    slots += name
                    out.append("(?<").append(name).append('>').append(inner).append(')')
                    index = slotEnd + 1
                }
                else -> {
                    out.append(char)
                    index++
                }
            }
        }
        return out.toString()
    }

    private fun alternation(name: String, lists: Map<String, Collection<String>>): String {
        val words = lists[name] ?: words(name)
        if (words.isEmpty()) return NEVER
        return words
            .sortedByDescending { it.length }
            .joinToString("|", "(?:", ")") { Regex.escape(it) }
    }

    companion object {
        private const val ANY = ".+?"
        private const val NEVER = "(?!)"
        private const val ENGLISH = "en"
        private const val WEEKDAYS = "weekday"
        private val NAME = Regex("[a-z@][a-z0-9.]*")
        private val SLOT = Regex("[a-z][a-zA-Z0-9]*")

        /** The device's languages plus English, which is always understood. */
        fun load(locales: List<Locale>): Lexicon {
            val languages = (locales.map { it.language } + ENGLISH).distinct()
            val sources =
                languages.mapNotNull { language -> read(language)?.let { language to it } }
            return merge(
                sources.map { it.first },
                sources.map { it.second },
                locales + Locale.ENGLISH,
            )
        }

        fun english(): Lexicon = load(listOf(Locale.ENGLISH))

        /** Builds a lexicon from file contents; for tests and language packs. */
        fun parse(vararg files: String): Lexicon =
            merge(files.indices.map { "test$it" }, files.toList(), listOf(Locale.ENGLISH))

        private fun read(language: String): String? =
            Lexicon::class
                .java
                .getResourceAsStream("/org/prauga/assistant/understanding/lexicon/$language.lex")
                ?.use { it.readBytes().toString(Charsets.UTF_8) }

        private fun merge(
            languages: List<String>,
            files: List<String>,
            locales: List<Locale>,
        ): Lexicon {
            val tables = HashMap<String, HashMap<String, Double>>()
            val intents = HashMap<String, MutableList<Template>>()
            files.forEach { parseInto(it, tables, intents) }
            val weekdays = tables.getOrPut(WEEKDAYS) { HashMap() }
            locales.distinct().forEach { locale ->
                DayOfWeek.entries.forEach { day ->
                    listOf(TextStyle.FULL, TextStyle.SHORT).forEach { style ->
                        val name = day.getDisplayName(style, locale).lowercase(locale).trimEnd('.')
                        weekdays.putIfAbsent(name, day.value.toDouble())
                    }
                }
            }
            return Lexicon(languages, tables, intents)
        }

        private fun parseInto(
            text: String,
            tables: HashMap<String, HashMap<String, Double>>,
            intents: HashMap<String, MutableList<Template>>,
        ) {
            var kind = ""
            var name = ""
            text.lineSequence().forEachIndexed { number, raw ->
                val line = raw.substringBefore(" #").trim()
                if (line.isEmpty() || line.startsWith("#")) return@forEachIndexed
                if (line.startsWith("[") && line.endsWith("]")) {
                    val header = line.substring(1, line.length - 1).trim().split(' ')
                    require(header.size == 2) { "Line ${number + 1}: expected [kind name]" }
                    kind = header[0]
                    name = header[1]
                    if (kind != "intent") tables.getOrPut(name) { HashMap() }
                    return@forEachIndexed
                }
                when (kind) {
                    "words" ->
                        line
                            .split('|')
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                            .forEach { tables.getValue(name).putIfAbsent(it, 0.0) }
                    "values" -> {
                        val word = line.substringBeforeLast('=').trim()
                        val value = line.substringAfterLast('=').trim().toDoubleOrNull()
                        require(word.isNotEmpty() && value != null) {
                            "Line ${number + 1}: expected word = number"
                        }
                        tables.getValue(name).putIfAbsent(word, value)
                    }
                    "intent" -> {
                        val confidence = line.substringBefore(' ').toFloatOrNull()
                        require(confidence != null && confidence in 0f..1f) {
                            "Line ${number + 1}: expected confidence then pattern"
                        }
                        intents.getOrPut(name) { ArrayList() } +=
                            Template(confidence, line.substringAfter(' ').trim())
                    }
                    else -> error("Line ${number + 1}: entry outside a section")
                }
            }
        }

        private fun listEnd(pattern: String, start: Int): Int {
            val end = pattern.indexOf('>', start)
            if (end < 0) return -1
            return end.takeIf { NAME.matches(pattern.substring(start + 1, end)) } ?: -1
        }

        private fun slotEnd(pattern: String, start: Int): Int {
            var depth = 0
            for (index in start until pattern.length) {
                when (pattern[index]) {
                    '{' -> depth++
                    '}' ->
                        if (--depth == 0) {
                            val name = pattern.substring(start + 1, index).substringBefore('=')
                            return index.takeIf { SLOT.matches(name) } ?: -1
                        }
                }
            }
            return -1
        }
    }
}
