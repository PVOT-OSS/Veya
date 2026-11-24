// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.api

import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class FileModelStore(
    private val directory: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ModelStore {
    private val state = MutableStateFlow<List<LocalModel>>(emptyList())

    override val models: StateFlow<List<LocalModel>> = state.asStateFlow()

    override suspend fun refresh() {
        state.value = withContext(dispatcher) { scan() }
    }

    override suspend fun import(
        fileName: String,
        expectedBytes: Long?,
        open: () -> InputStream,
    ): ImportResult =
        withContext(dispatcher) {
            val name = File(fileName).name
            val format =
                ModelFormat.forFileName(name) ?: return@withContext ImportResult.UnsupportedFormat
            directory.mkdirs()
            val target = File(directory, name)
            if (target.exists()) return@withContext ImportResult.AlreadyExists
            val available = directory.usableSpace
            if (expectedBytes != null && expectedBytes + SPACE_MARGIN_BYTES > available) {
                return@withContext ImportResult.InsufficientSpace(expectedBytes, available)
            }
            val partial = File(directory, "$name$PARTIAL_SUFFIX")
            try {
                open().use { input ->
                    partial.outputStream().use { output -> input.copyTo(output, BUFFER_BYTES) }
                }
                if (!partial.renameTo(target))
                    throw IOException("Could not move the model into place")
                val model = target.toModel(format)
                state.value = scan()
                ImportResult.Imported(model)
            } catch (e: IOException) {
                partial.delete()
                ImportResult.Failed(e.message)
            }
        }

    override suspend fun delete(id: String) {
        withContext(dispatcher) {
            scan().firstOrNull { it.id == id }?.let { File(it.path).delete() }
            state.value = scan()
        }
    }

    private fun scan(): List<LocalModel> =
        directory
            .listFiles()
            .orEmpty()
            .filter { it.isFile && !it.name.endsWith(PARTIAL_SUFFIX) }
            .mapNotNull { file -> ModelFormat.forFileName(file.name)?.let { file.toModel(it) } }
            .sortedBy { it.displayName.lowercase() }

    private fun File.toModel(format: ModelFormat) =
        LocalModel(
            id = name,
            displayName = nameWithoutExtension,
            path = absolutePath,
            sizeBytes = length(),
            format = format,
        )

    private companion object {
        const val PARTIAL_SUFFIX = ".partial"
        const val BUFFER_BYTES = 1 shl 20
        const val SPACE_MARGIN_BYTES = 256L shl 20
    }
}
