// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.litert

import org.prauga.assistant.inference.api.InferencePrompt
import org.prauga.assistant.inference.api.PromptTurn

internal data class ConversationTranscript(
    val systemInstruction: String?,
    val turns: List<PromptTurn>,
)

internal sealed interface ConversationPlan {
    data object Reuse : ConversationPlan

    data class Rebuild(val systemInstruction: String?, val history: List<PromptTurn>) :
        ConversationPlan

    companion object {
        fun decide(current: ConversationTranscript?, prompt: InferencePrompt): ConversationPlan =
            if (
                current != null &&
                    current.systemInstruction == prompt.systemInstruction &&
                    current.turns == prompt.history
            ) {
                Reuse
            } else {
                Rebuild(prompt.systemInstruction, prompt.history)
            }
    }
}
