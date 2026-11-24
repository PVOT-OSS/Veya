// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.litert

import org.junit.Assert.assertEquals
import org.junit.Test
import org.prauga.assistant.inference.api.InferencePrompt
import org.prauga.assistant.inference.api.PromptTurn

class ConversationPlanTest {
    private val turns = listOf(PromptTurn(true, "hi"), PromptTurn(false, "hello"))

    @Test
    fun reusesWhenTheConversationContinues() {
        val current = ConversationTranscript("be brief", turns)
        assertEquals(
            ConversationPlan.Reuse,
            ConversationPlan.decide(current, InferencePrompt("be brief", turns, "next")),
        )
    }

    @Test
    fun rebuildsWhenNothingIsOpen() {
        val plan = ConversationPlan.decide(null, InferencePrompt(null, turns, "next"))
        assertEquals(ConversationPlan.Rebuild(null, turns), plan)
    }

    @Test
    fun rebuildsWhenHistoryOrInstructionDiffers() {
        val current = ConversationTranscript("be brief", turns)
        assertEquals(
            ConversationPlan.Rebuild("be brief", turns.take(1)),
            ConversationPlan.decide(current, InferencePrompt("be brief", turns.take(1), "next")),
        )
        assertEquals(
            ConversationPlan.Rebuild("be chatty", turns),
            ConversationPlan.decide(current, InferencePrompt("be chatty", turns, "next")),
        )
    }
}
