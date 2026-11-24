// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.api

import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.io.path.createTempDirectory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileModelStoreTest {
    private val dir: File = createTempDirectory("models").toFile()
    private val store = FileModelStore(dir, Dispatchers.Unconfined)

    @After
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private fun bytes(size: Int) = { ByteArrayInputStream(ByteArray(size)) }

    @Test
    fun importsAndListsModels() = runTest {
        val result =
            store.import("content/Qwen3-0.6B.litertlm", 1_024, bytes(1_024))
                as ImportResult.Imported
        assertEquals("Qwen3-0.6B", result.model.displayName)
        assertEquals(1_024L, result.model.sizeBytes)
        assertEquals(listOf("Qwen3-0.6B.litertlm"), store.models.value.map { it.id })
    }

    @Test
    fun rejectsUnsupportedFiles() = runTest {
        assertEquals(ImportResult.UnsupportedFormat, store.import("model.gguf", 10, bytes(10)))
        assertTrue(store.models.value.isEmpty())
    }

    @Test
    fun refusesDuplicates() = runTest {
        store.import("a.litertlm", 1, bytes(1))
        assertEquals(ImportResult.AlreadyExists, store.import("a.litertlm", 1, bytes(1)))
    }

    @Test
    fun checksFreeSpaceBeforeCopying() = runTest {
        val result = store.import("huge.litertlm", Long.MAX_VALUE / 2, bytes(1))
        assertTrue(result is ImportResult.InsufficientSpace)
        assertFalse(File(dir, "huge.litertlm").exists())
    }

    @Test
    fun removesPartialFilesWhenCopyFails() = runTest {
        val failing = {
            object : InputStream() {
                override fun read(): Int = throw IOException("disconnected")
            }
        }
        assertTrue(store.import("broken.litertlm", null, failing) is ImportResult.Failed)
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun deletesModels() = runTest {
        store.import("a.litertlm", 1, bytes(1))
        store.delete("a.litertlm")
        assertTrue(store.models.value.isEmpty())
        store.refresh()
        assertTrue(store.models.value.isEmpty())
    }

    @Test
    fun refreshFindsFilesAlreadyOnDisk() = runTest {
        File(dir, "b.litertlm").writeBytes(ByteArray(3))
        File(dir, "notes.txt").writeText("x")
        store.refresh()
        assertEquals(listOf("b.litertlm"), store.models.value.map { it.id })
    }
}
