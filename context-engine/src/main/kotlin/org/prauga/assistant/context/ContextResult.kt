// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.context

data class ContextResult(val items: List<ContextItem>, val skipped: List<SkippedProvider>)

data class SkippedProvider(val providerId: String, val reason: SkipReason)

enum class SkipReason {
    NOT_GRANTED,
    TYPE_NOT_OFFERED,
    TIMED_OUT,
    FAILED,
}
