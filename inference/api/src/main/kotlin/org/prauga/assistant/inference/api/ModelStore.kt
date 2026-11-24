// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.api

import java.io.InputStream
import kotlinx.coroutines.flow.StateFlow

interface ModelStore {
    val models: StateFlow<List<LocalModel>>

    suspend fun refresh()

    suspend fun import(
        fileName: String,
        expectedBytes: Long?,
        open: () -> InputStream,
    ): ImportResult

    suspend fun delete(id: String)
}

sealed interface ImportResult {
    data class Imported(val model: LocalModel) : ImportResult

    data object UnsupportedFormat : ImportResult

    data object AlreadyExists : ImportResult

    data class InsufficientSpace(val requiredBytes: Long, val availableBytes: Long) : ImportResult

    data class Failed(val message: String?) : ImportResult
}
