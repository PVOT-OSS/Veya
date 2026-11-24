// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.home

sealed interface HomeEvent {
    data class AddItem(val title: String) : HomeEvent

    data object Retry : HomeEvent
}
