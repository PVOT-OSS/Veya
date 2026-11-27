// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

import kotlin.math.abs
import org.prauga.assistant.understanding.Phrase

class PatternMatcher(private val intentId: String, private val rules: List<Rule>) {
    data class Rule(
        val pattern: Regex,
        val confidence: Float,
        val entities: List<String> = emptyList(),
        val required: List<List<String>> = emptyList(),
    ) {
        fun mayMatch(text: String): Boolean = Phrase.satisfied(required, text)
    }

    fun match(utterance: Utterance): IntentMatch? {
        var best: IntentMatch? = null
        for (rule in rules) {
            if (best != null && best.confidence >= rule.confidence) continue
            if (!rule.mayMatch(utterance.normalized)) continue
            val result = rule.pattern.matchEntire(utterance.normalized) ?: continue
            val entities =
                rule.entities
                    .mapNotNull { name ->
                        result.groups[name]
                            ?.value
                            ?.trim()
                            ?.takeIf { it.isNotEmpty() }
                            ?.let { name to it }
                    }
                    .toMap()
            best = IntentMatch(intentId, rule.confidence, entities)
        }
        return best
    }

    /**
     * The share of a phrase's required words found in [utterance], for the closest phrase. Words of
     * four letters or more also count with one typo ("alrm" for "alarm").
     */
    fun closeness(utterance: Utterance): Float {
        val text = utterance.normalized
        val tokens = text.split(' ')
        var best = 0f
        for (rule in rules) {
            if (rule.required.isEmpty()) continue
            val found = rule.required.count { group -> group.any { present(it, text, tokens) } }
            best = maxOf(best, found.toFloat() / rule.required.size)
        }
        return best
    }

    private fun present(word: String, text: String, tokens: List<String>): Boolean {
        if (' ' in word) return Regex("\\b" + Regex.escape(word) + "\\b").containsMatchIn(text)
        return tokens.any { it == word || (word.length >= FUZZY_FROM && oneEditApart(it, word)) }
    }

    private fun oneEditApart(a: String, b: String): Boolean {
        if (abs(a.length - b.length) > 1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) {
                i++
                j++
                continue
            }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> {
                    i++
                    j++
                }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }

    companion object {
        private const val FUZZY_FROM = 4

        fun from(intentId: String, phrases: List<Phrase>) =
            PatternMatcher(
                intentId,
                phrases.map { Rule(it.pattern, it.confidence, it.slots, it.required) },
            )

        fun rule(pattern: String, confidence: Float, vararg entities: String) =
            Rule(Regex(pattern), confidence, entities.toList())
    }
}
