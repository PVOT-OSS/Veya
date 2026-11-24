// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.litert

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Debug
import android.os.PowerManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.inference.api.ComputeBackend
import org.prauga.assistant.inference.api.InferenceConfig
import org.prauga.assistant.inference.api.InferenceEvent
import org.prauga.assistant.inference.api.InferenceException
import org.prauga.assistant.inference.api.InferencePrompt
import org.prauga.assistant.inference.api.LocalModel
import org.prauga.assistant.inference.api.ModelFormat

@RunWith(AndroidJUnit4::class)
class ModelSpeedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val arguments: Bundle = InstrumentationRegistry.getArguments()

    @Test
    fun compareModels() = runBlocking {
        val directory = File(arguments.getString(ARG_DIR) ?: DEFAULT_DIR)
        val models =
            directory
                .listFiles()
                .orEmpty()
                .filter { ModelFormat.forFileName(it.name) != null }
                .sortedBy { it.length() }
        assumeTrue("No .litertlm files in $directory", models.isNotEmpty())
        val backends =
            (arguments.getString(ARG_BACKENDS) ?: "cpu,gpu").split(',').map {
                ComputeBackend.valueOf(it.trim().uppercase())
            }
        val runs = arguments.getString(ARG_RUNS)?.toInt() ?: DEFAULT_RUNS
        val speculative = arguments.getString(ARG_MTP).toBoolean()
        report("device", deviceLine())
        for (file in models) {
            val model =
                LocalModel(
                    file.name,
                    file.nameWithoutExtension,
                    file.absolutePath,
                    file.length(),
                    ModelFormat.LITERT_LM,
                )
            for (backend in backends) {
                report(
                    "result",
                    measure(
                        model,
                        InferenceConfig(
                            backend,
                            maxOutputTokens = MAX_OUTPUT,
                            speculativeDecoding = speculative,
                        ),
                        runs,
                    ),
                )
            }
        }
    }

    private suspend fun measure(model: LocalModel, config: InferenceConfig, runs: Int): String {
        val engine =
            LiteRtInferenceEngine(context.cacheDir.path, context.applicationInfo.nativeLibraryDir)
        val label =
            "model=${model.id} sizeMb=${model.sizeBytes / BYTES_PER_MB} backend=${config.backend} mtp=${config.speculativeDecoding}"
        val pssBefore = pssMb()
        return try {
            val loadStart = System.nanoTime()
            engine.load(model, config)
            val loadMillis = (System.nanoTime() - loadStart) / NANOS_PER_MILLI
            val pssLoaded = pssMb()
            engine.generate(InferencePrompt(null, emptyList(), WARM_UP)).toList()
            val samples =
                (1..runs).flatMap {
                    PROMPTS.map { text ->
                        engine
                            .generate(InferencePrompt(null, emptyList(), text))
                            .toList()
                            .filterIsInstance<InferenceEvent.Done>()
                            .single()
                            .stats
                    }
                }
            val ttft = samples.mapNotNull { it.timeToFirstChunkMillis }.sorted()
            val decode = samples.mapNotNull { it.decodeTokensPerSecond }.sorted()
            "$label loadMs=$loadMillis firstLoad=${model.id !in warmed} ttftMedianMs=${ttft.median()} ttftMaxMs=${ttft.lastOrNull()} " +
                "decodeTokPerSecMedian=${"%.1f".format(decode.medianDouble())} samples=${samples.size} " +
                "pssBeforeMb=$pssBefore pssLoadedMb=$pssLoaded pssAfterMb=${pssMb()} thermal=${thermal()}"
        } catch (e: InferenceException) {
            "$label failed=${e.message}"
        } finally {
            warmed += model.id
            engine.unload()
        }
    }

    private fun report(key: String, line: String) {
        instrumentation.sendStatus(0, Bundle().apply { putString(key, line) })
    }

    private fun deviceLine(): String {
        val memory =
            ActivityManager.MemoryInfo().also {
                context.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
            }
        val soc =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}"
            else Build.HARDWARE
        return "model=${Build.MODEL} soc=$soc sdk=${Build.VERSION.SDK_INT} ramMb=${memory.totalMem / BYTES_PER_MB}"
    }

    private fun pssMb(): Long = Debug.getPss() / KB_PER_MB

    private fun thermal(): Int =
        context.getSystemService(PowerManager::class.java).currentThermalStatus

    private fun List<Long>.median(): Long? = if (isEmpty()) null else this[size / 2]

    private fun List<Double>.medianDouble(): Double = if (isEmpty()) Double.NaN else this[size / 2]

    private companion object {
        val warmed = mutableSetOf<String>()
        const val ARG_DIR = "dir"
        const val ARG_BACKENDS = "backends"
        const val ARG_RUNS = "runs"
        const val ARG_MTP = "mtp"
        const val DEFAULT_DIR = "/data/local/tmp/veya-models"
        const val DEFAULT_RUNS = 3
        const val MAX_OUTPUT = 128
        const val BYTES_PER_MB = 1_048_576L
        const val KB_PER_MB = 1_024L
        const val NANOS_PER_MILLI = 1_000_000L
        const val WARM_UP = "Say hello."
        val PROMPTS =
            listOf(
                "In one sentence, what does a phone's flashlight do?",
                "Give me three short tips for saving battery on a phone.",
            )
    }
}
