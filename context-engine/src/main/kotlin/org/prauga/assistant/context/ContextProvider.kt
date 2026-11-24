// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.context

interface ContextProvider {
    val descriptor: ContextProviderDescriptor

    suspend fun query(query: ContextQuery): List<ContextItem>
}

data class ContextProviderDescriptor(
    val id: String,
    val packageName: String,
    val label: String,
    val types: Set<String>,
    val maxSensitivity: Sensitivity,
    val vocabulary: List<ContextTypeInfo> = emptyList(),
)

data class ContextTypeInfo(
    val id: String,
    val label: String,
    val synonyms: List<String>,
    val flags: List<ContextFlagInfo> = emptyList(),
)

data class ContextFlagInfo(val key: String, val synonyms: List<String>)

class ContextProviderException(message: String?, cause: Throwable? = null) :
    Exception(message, cause)
