// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

data class ContextItem(
    val id: String,
    val type: String,
    val title: String,
    val timestamp: Long,
    val sensitivity: Sensitivity,
    val content: String = "",
    val expiresAt: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
)

enum class Sensitivity {
    PUBLIC,
    PERSONAL,
    SENSITIVE,
}

data class ContextQuery(
    val text: String?,
    val types: Set<String>,
    val since: Long?,
    val limit: Int,
)
