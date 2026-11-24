// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.api

data class LocalModel(
    val id: String,
    val displayName: String,
    val path: String,
    val sizeBytes: Long,
    val format: ModelFormat,
)

enum class ModelFormat(val extension: String) {
    LITERT_LM("litertlm");

    companion object {
        fun forFileName(name: String): ModelFormat? =
            entries.firstOrNull { name.endsWith(".${it.extension}", ignoreCase = true) }
    }
}

enum class ComputeBackend {
    CPU,
    GPU,
    NPU,
}
