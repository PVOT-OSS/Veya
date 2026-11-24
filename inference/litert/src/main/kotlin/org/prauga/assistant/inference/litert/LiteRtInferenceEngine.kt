// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.litert

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.LiteRtLmJniException
import com.google.ai.edge.litertlm.Message
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.SpeculativeDecodingConfig
import com.google.ai.edge.litertlm.SpeculativeDecodingMode
import java.util.concurrent.Executors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.prauga.assistant.inference.api.ComputeBackend
import org.prauga.assistant.inference.api.EngineState
import org.prauga.assistant.inference.api.InferenceConfig
import org.prauga.assistant.inference.api.InferenceEngine
import org.prauga.assistant.inference.api.InferenceEvent
import org.prauga.assistant.inference.api.InferenceException
import org.prauga.assistant.inference.api.InferencePrompt
import org.prauga.assistant.inference.api.InferenceTimer
import org.prauga.assistant.inference.api.LocalModel
import org.prauga.assistant.inference.api.ModelFormat
import org.prauga.assistant.inference.api.PromptTurn
import org.prauga.assistant.inference.api.RuntimeInfo

@OptIn(ExperimentalApi::class)
class LiteRtInferenceEngine(private val cacheDir: String, private val nativeLibraryDir: String) :
    InferenceEngine {
    override val runtime =
        RuntimeInfo(
            id = "litert-lm",
            displayName = "LiteRT-LM",
            formats = setOf(ModelFormat.LITERT_LM),
            backends = setOf(ComputeBackend.CPU, ComputeBackend.GPU, ComputeBackend.NPU),
        )

    private val dispatcher =
        Executors.newSingleThreadExecutor { Thread(it, THREAD_NAME) }.asCoroutineDispatcher()
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow<EngineState>(EngineState.Unloaded)
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var transcript: ConversationTranscript? = null
    private var sampling: InferenceConfig? = null

    override val state: StateFlow<EngineState> = mutableState.asStateFlow()

    override suspend fun load(model: LocalModel, config: InferenceConfig) {
        withContext(dispatcher) {
            mutex.withLock {
                release()
                mutableState.value = EngineState.Loading(model, config)
                ExperimentalFlags.enableBenchmark = true
                val timer = InferenceTimer()
                val created =
                    try {
                        Engine(engineConfig(model, config)).also { it.initialize() }
                    } catch (e: LiteRtLmJniException) {
                        fail(model, config, e)
                    } catch (e: IllegalStateException) {
                        fail(model, config, e)
                    } catch (e: IllegalArgumentException) {
                        fail(model, config, e)
                    }
                engine = created
                sampling = config
                mutableState.value =
                    EngineState.Ready(model, config, timer.finish(null).totalMillis)
            }
        }
    }

    override suspend fun unload() {
        withContext(dispatcher) { mutex.withLock { release() } }
    }

    override fun generate(prompt: InferencePrompt): Flow<InferenceEvent> =
        flow<InferenceEvent> {
                mutex.withLock {
                    val active = conversationFor(prompt)
                    val timer = InferenceTimer()
                    val reply = StringBuilder()
                    try {
                        active.sendMessageAsync(prompt.userText).collect { message ->
                            val text = message.toString()
                            if (text.isNotEmpty()) {
                                timer.onChunk()
                                reply.append(text)
                                emit(InferenceEvent.Chunk(text))
                            }
                        }
                    } catch (e: CancellationException) {
                        active.cancelProcess()
                        transcript = null
                        throw e
                    } catch (e: LiteRtLmJniException) {
                        transcript = null
                        throw InferenceException(e.message, e)
                    }
                    transcript =
                        ConversationTranscript(
                            prompt.systemInstruction,
                            prompt.history +
                                PromptTurn(true, prompt.userText) +
                                PromptTurn(false, reply.toString()),
                        )
                    val decoded = active.getBenchmarkInfo().lastDecodeTokenCount.takeIf { it > 0 }
                    emit(InferenceEvent.Done(timer.finish(decoded)))
                }
            }
            .flowOn(dispatcher)

    override suspend fun cancel() {
        withContext(dispatcher) { conversation?.cancelProcess() }
    }

    private fun conversationFor(prompt: InferencePrompt): Conversation {
        val current = conversation
        val plan = ConversationPlan.decide(transcript, prompt)
        if (plan == ConversationPlan.Reuse && current != null) return current
        val loaded = engine ?: throw InferenceException("No model is loaded")
        current?.close()
        conversation = null
        transcript = null
        val rebuild =
            plan as? ConversationPlan.Rebuild
                ?: ConversationPlan.Rebuild(prompt.systemInstruction, prompt.history)
        val created = loaded.createConversation(conversationConfig(rebuild))
        conversation = created
        transcript = ConversationTranscript(rebuild.systemInstruction, rebuild.history)
        return created
    }

    private fun conversationConfig(plan: ConversationPlan.Rebuild): ConversationConfig {
        val samplerConfig =
            sampling?.sampling?.let { SamplerConfig(it.topK, it.topP, it.temperature) }
        return ConversationConfig(
            systemInstruction = plan.systemInstruction?.let { Contents.of(it) },
            initialMessages =
                plan.history.map {
                    if (it.fromUser) Message.user(it.text) else Message.model(it.text)
                },
            samplerConfig = samplerConfig,
        )
    }

    private fun engineConfig(model: LocalModel, config: InferenceConfig): EngineConfig =
        EngineConfig(
            modelPath = model.path,
            backend = backend(config.backend),
            maxNumTokens = config.maxContextTokens,
            cacheDir = cacheDir,
            speculativeDecodingConfig =
                if (config.speculativeDecoding)
                    SpeculativeDecodingConfig(SpeculativeDecodingMode.ON)
                else null,
        )

    private fun backend(backend: ComputeBackend): Backend =
        when (backend) {
            ComputeBackend.CPU -> Backend.CPU()
            ComputeBackend.GPU -> Backend.GPU()
            ComputeBackend.NPU -> Backend.NPU(nativeLibraryDir)
        }

    private fun fail(model: LocalModel, config: InferenceConfig, cause: Exception): Nothing {
        mutableState.value = EngineState.Failed(model, config, cause.message)
        throw InferenceException(cause.message, cause)
    }

    private fun release() {
        conversation?.close()
        conversation = null
        transcript = null
        engine?.close()
        engine = null
        sampling = null
        mutableState.value = EngineState.Unloaded
    }

    private companion object {
        const val THREAD_NAME = "litert-lm"
    }
}
