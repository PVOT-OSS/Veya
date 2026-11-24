// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

class PatternMatcher(private val intentId: String, private val rules: List<Rule>) {
    data class Rule(
        val pattern: Regex,
        val confidence: Float,
        val entities: List<String> = emptyList(),
    )

    fun match(utterance: Utterance): IntentMatch? {
        var best: IntentMatch? = null
        for (rule in rules) {
            val result = rule.pattern.matchEntire(utterance.normalized) ?: continue
            if (best != null && best.confidence >= rule.confidence) continue
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
        fun rule(pattern: String, confidence: Float, vararg entities: String) =
            Rule(Regex(pattern), confidence, entities.toList())
    }
}
