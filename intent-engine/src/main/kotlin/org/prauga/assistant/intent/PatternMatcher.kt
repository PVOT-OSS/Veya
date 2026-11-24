// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

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

    companion object {
        fun from(intentId: String, phrases: List<Phrase>) =
            PatternMatcher(
                intentId,
                phrases.map { Rule(it.pattern, it.confidence, it.slots, it.required) },
            )

        fun rule(pattern: String, confidence: Float, vararg entities: String) =
            Rule(Regex(pattern), confidence, entities.toList())
    }
}
