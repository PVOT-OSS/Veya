// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.context

data class ContextItem(
    val id: String,
    val type: String,
    val title: String,
    val content: String,
    val source: ContextSource,
    val timestamp: Long,
    val expiresAt: Long? = null,
    val sensitivity: Sensitivity = Sensitivity.PERSONAL,
    val metadata: Map<String, String> = emptyMap(),
)

data class ContextSource(val packageName: String, val label: String)

enum class Sensitivity {
    PUBLIC,
    PERSONAL,
    SENSITIVE,
}
