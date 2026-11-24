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
)

class ContextProviderException(message: String?, cause: Throwable? = null) :
    Exception(message, cause)
