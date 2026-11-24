// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.prauga.assistant.IoDispatcher
import org.prauga.assistant.core.ConversationStore
import org.prauga.assistant.core.ConversationSummary

@HiltViewModel
class HistoryViewModel
@Inject
constructor(
    private val store: ConversationStore,
    @param:IoDispatcher private val io: CoroutineDispatcher,
) : ViewModel() {
    private val attempt = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<UiState<List<ConversationSummary>>> =
        attempt
            .flatMapLatest {
                store
                    .conversations()
                    .map<List<ConversationSummary>, UiState<List<ConversationSummary>>> {
                        UiState.Success(it)
                    }
                    .catch { emit(UiState.Error(it.message.orEmpty(), it)) }
            }
            .flowOn(io)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                UiState.Loading,
            )

    fun onEvent(event: HistoryEvent) {
        when (event) {
            is HistoryEvent.Delete ->
                viewModelScope.launch { withContext(io) { store.delete(event.conversationId) } }
            HistoryEvent.Retry -> attempt.value++
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
