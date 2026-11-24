// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.conversation

import org.prauga.assistant.core.IntentChoice

sealed interface ConversationEvent {
    data class Submit(val text: String) : ConversationEvent

    data class Choose(val choice: IntentChoice) : ConversationEvent

    data class PermissionResult(val choice: IntentChoice, val granted: Boolean) : ConversationEvent

    data object DismissPrompt : ConversationEvent

    data object NewConversation : ConversationEvent

    data class Open(val conversationId: String) : ConversationEvent
}
