// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

data class ContextType(
    val id: String,
    val label: String,
    val synonyms: List<String> = emptyList(),
    val flags: List<ContextFlag> = emptyList(),
)

data class ContextFlag(val key: String, val synonyms: List<String>)
