// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.inference.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface InferenceEngine {
    val runtime: RuntimeInfo
    val state: StateFlow<EngineState>

    suspend fun load(model: LocalModel, config: InferenceConfig)

    suspend fun unload()

    fun generate(prompt: InferencePrompt): Flow<InferenceEvent>

    suspend fun cancel()
}

data class RuntimeInfo(
    val id: String,
    val displayName: String,
    val formats: Set<ModelFormat>,
    val backends: Set<ComputeBackend>,
)

data class InferenceConfig(
    val backend: ComputeBackend = ComputeBackend.CPU,
    val maxContextTokens: Int? = null,
    val maxOutputTokens: Int? = null,
    val speculativeDecoding: Boolean = false,
    val thinking: Boolean = false,
    val sampling: Sampling? = null,
)

data class Sampling(val topK: Int, val topP: Double, val temperature: Double)

sealed interface EngineState {
    data object Unloaded : EngineState

    data class Loading(val model: LocalModel, val config: InferenceConfig) : EngineState

    data class Ready(val model: LocalModel, val config: InferenceConfig, val loadMillis: Long) :
        EngineState

    data class Failed(val model: LocalModel, val config: InferenceConfig, val message: String?) :
        EngineState
}

data class InferencePrompt(
    val systemInstruction: String?,
    val history: List<PromptTurn>,
    val userText: String,
)

data class PromptTurn(val fromUser: Boolean, val text: String)

sealed interface InferenceEvent {
    data class Chunk(val text: String) : InferenceEvent

    data class Done(val stats: InferenceStats) : InferenceEvent
}

class InferenceException(message: String?, cause: Throwable? = null) : Exception(message, cause)
