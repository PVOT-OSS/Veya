// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.conversation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.assistant.IoDispatcher
import org.prauga.assistant.core.Assistant
import org.prauga.assistant.core.AssistantEvent
import org.prauga.assistant.core.ConversationStore
import org.prauga.assistant.core.IntentChoice
import org.prauga.assistant.core.MessageKind
import org.prauga.assistant.core.StoredMessage
import org.prauga.assistant.platform.binder.ContextWarmUp

@HiltViewModel
class ConversationViewModel
@Inject
constructor(
    private val assistant: Assistant,
    private val store: ConversationStore,
    @param:IoDispatcher private val io: CoroutineDispatcher,
    private val savedState: SavedStateHandle,
    private val contextWarmUp: ContextWarmUp,
) : ViewModel() {
    private val conversationId: StateFlow<String?> = savedState.getStateFlow(KEY_CONVERSATION, null)
    private val session = MutableStateFlow(Session())
    private var turn: Job? = null

    init {
        contextWarmUp.warmUp()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<ConversationUiState> =
        combine(
                conversationId,
                conversationId.flatMapLatest { id ->
                    id?.let(store::messages) ?: flowOf(emptyList())
                },
                session,
            ) { id, messages, current ->
                ConversationUiState(
                    conversationId = id,
                    entries = messages.map { it.toEntry() },
                    prompt = current.prompt,
                    busy = current.busy,
                    lastReplyLatencyMillis = current.lastReplyLatencyMillis,
                )
            }
            .flowOn(io)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                ConversationUiState(),
            )

    fun onEvent(event: ConversationEvent) {
        when (event) {
            is ConversationEvent.Submit -> {
                val text = event.text.trim()
                if (text.isEmpty() || session.value.busy) return
                val pending = session.value.prompt as? ConversationPrompt.MissingDetail
                val id = conversationId.value
                if (pending != null && id != null) {
                    run(assistant.answer(id, pending.choice, pending.parameter, text))
                } else {
                    run(assistant.submit(id, text))
                }
            }
            is ConversationEvent.Choose -> chooseIn(event.choice)
            is ConversationEvent.PermissionResult ->
                if (event.granted) {
                    chooseIn(event.choice)
                } else {
                    session.update { it.copy(prompt = ConversationPrompt.PermissionDenied) }
                }
            ConversationEvent.DismissPrompt -> session.update { it.copy(prompt = null) }
            ConversationEvent.NewConversation -> switchTo(null)
            is ConversationEvent.Open -> switchTo(event.conversationId)
        }
    }

    private fun chooseIn(choice: IntentChoice) {
        val id = conversationId.value ?: return
        if (session.value.busy) return
        run(assistant.choose(id, choice))
    }

    private fun switchTo(id: String?) {
        turn?.cancel()
        session.value = Session()
        savedState[KEY_CONVERSATION] = id
    }

    private fun run(events: Flow<AssistantEvent>) {
        turn?.cancel()
        session.update { it.copy(busy = true, prompt = null, lastReplyLatencyMillis = null) }
        turn =
            viewModelScope.launch {
                try {
                    events.flowOn(io).collect(::handle)
                } finally {
                    session.update { it.copy(busy = false) }
                }
            }
    }

    private fun handle(event: AssistantEvent) {
        when (event) {
            is AssistantEvent.TurnStarted -> savedState[KEY_CONVERSATION] = event.conversationId
            is AssistantEvent.Reply ->
                session.update { it.copy(lastReplyLatencyMillis = event.elapsedMillis) }
            is AssistantEvent.ActionFailed -> Unit
            is AssistantEvent.Unanswered -> Unit
            is AssistantEvent.NeedsPermission ->
                session.update {
                    it.copy(prompt = ConversationPrompt.Permission(event.choice, event.permissions))
                }
            is AssistantEvent.Clarify ->
                session.update { it.copy(prompt = ConversationPrompt.Clarify(event.choices)) }
            is AssistantEvent.MissingDetail ->
                session.update {
                    it.copy(
                        prompt =
                            ConversationPrompt.MissingDetail(
                                event.choice,
                                event.parameter.name,
                                event.parameter.question,
                            )
                    )
                }
        }
    }

    private fun StoredMessage.toEntry(): ConversationEntry =
        when (kind) {
            MessageKind.USER -> ConversationEntry.User(id, text)
            MessageKind.REPLY -> ConversationEntry.Reply(id, text)
            MessageKind.FAILURE -> ConversationEntry.Failure(id, text)
            MessageKind.UNANSWERED -> ConversationEntry.Unanswered(id)
        }

    private data class Session(
        val prompt: ConversationPrompt? = null,
        val busy: Boolean = false,
        val lastReplyLatencyMillis: Long? = null,
    )

    private companion object {
        const val KEY_CONVERSATION = "conversation"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
