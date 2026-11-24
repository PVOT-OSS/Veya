// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.history

sealed interface HistoryEvent {
    data class Delete(val conversationId: String) : HistoryEvent

    data object Retry : HistoryEvent
}
