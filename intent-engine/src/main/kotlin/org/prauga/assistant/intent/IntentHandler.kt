// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

interface IntentHandler {
    val descriptor: IntentDescriptor

    fun match(utterance: Utterance): IntentMatch?

    suspend fun execute(match: IntentMatch): IntentOutcome
}

data class IntentDescriptor(
    val id: String,
    val displayName: String,
    val parameters: List<IntentParameter> = emptyList(),
    val requiredPermissions: Set<String> = emptySet(),
    val minSdk: Int = 1,
    val maxSdk: Int? = null,
) {
    fun supports(sdkInt: Int): Boolean = sdkInt >= minSdk && (maxSdk == null || sdkInt <= maxSdk)
}

data class IntentParameter(val name: String, val required: Boolean)

data class IntentMatch(
    val intentId: String,
    val confidence: Float,
    val entities: Map<String, String> = emptyMap(),
) {
    init {
        require(confidence in 0f..1f) { "Confidence must be between 0 and 1" }
    }
}

sealed interface IntentOutcome {
    data class Reply(val text: String) : IntentOutcome

    data class Failed(val text: String) : IntentOutcome
}
