// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

interface ContextProvider {
    val types: Set<String>

    val maxSensitivity: Sensitivity

    fun query(query: ContextQuery): List<ContextItem>
}
