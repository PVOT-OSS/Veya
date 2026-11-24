// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

class IntentEngine(
    handlers: List<IntentHandler>,
    private val gate: PlatformGate,
    private val fillers: Utterance.Fillers = Utterance.Fillers.NONE,
    private val acceptThreshold: Float = DEFAULT_ACCEPT_THRESHOLD,
    private val ambiguityMargin: Float = DEFAULT_AMBIGUITY_MARGIN,
) {
    private val handlers: List<IntentHandler> =
        handlers.filter { it.descriptor.supports(gate.sdkInt) }

    init {
        val ids = handlers.map { it.descriptor.id }
        require(ids.size == ids.toSet().size) { "Intent ids must be unique" }
    }

    val registered: List<IntentDescriptor>
        get() = handlers.map { it.descriptor }

    fun resolve(text: String): Resolution {
        val utterance = Utterance(text, fillers)
        if (utterance.normalized.isEmpty()) return Resolution.NoMatch
        val candidates =
            handlers
                .mapNotNull { handler -> handler.match(utterance)?.let { Candidate(handler, it) } }
                .filter { it.match.confidence >= acceptThreshold }
                .sortedByDescending { it.match.confidence }
        val top = candidates.firstOrNull() ?: return Resolution.NoMatch
        val close =
            candidates.filter { top.match.confidence - it.match.confidence < ambiguityMargin }
        if (close.size > 1) return Resolution.Ambiguous(close)
        return check(top)
    }

    fun resolve(intentId: String, entities: Map<String, String>): Resolution {
        val handler =
            handlers.firstOrNull { it.descriptor.id == intentId } ?: return Resolution.NoMatch
        return check(Candidate(handler, IntentMatch(intentId, 1f, entities)))
    }

    private fun check(candidate: Candidate): Resolution {
        val descriptor = candidate.handler.descriptor
        val missingParameter =
            descriptor.parameters.firstOrNull {
                it.required && candidate.match.entities[it.name].isNullOrBlank()
            }
        if (missingParameter != null)
            return Resolution.MissingParameter(candidate, missingParameter)
        val missingPermissions = descriptor.requiredPermissions.filterNot(gate::isGranted).toSet()
        if (missingPermissions.isNotEmpty())
            return Resolution.NeedsPermission(candidate, missingPermissions)
        return Resolution.Matched(candidate)
    }

    companion object {
        const val DEFAULT_ACCEPT_THRESHOLD = 0.6f
        const val DEFAULT_AMBIGUITY_MARGIN = 0.1f
    }
}

data class Candidate(val handler: IntentHandler, val match: IntentMatch)

sealed interface Resolution {
    data class Matched(val candidate: Candidate) : Resolution

    data class Ambiguous(val candidates: List<Candidate>) : Resolution

    data class NeedsPermission(val candidate: Candidate, val permissions: Set<String>) : Resolution

    data class MissingParameter(val candidate: Candidate, val parameter: IntentParameter) :
        Resolution

    data object NoMatch : Resolution
}
