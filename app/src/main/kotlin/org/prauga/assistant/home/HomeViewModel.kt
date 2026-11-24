// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import org.prauga.assistant.IoDispatcher
import org.prauga.assistant.capabilities.CapabilityRepository
import org.prauga.assistant.core.ConversationStore
import org.prauga.assistant.platform.binder.IntegrationGrants
import org.prauga.assistant.platform.binder.LiveProviders

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    capabilities: CapabilityRepository,
    live: LiveProviders,
    grants: IntegrationGrants,
    store: ConversationStore,
    @param:IoDispatcher io: CoroutineDispatcher,
) : ViewModel() {
    val state: StateFlow<HomeUiState> =
        combine(
                capabilities.map,
                live.available,
                grants.all,
                store.conversations().flowOn(io),
                ::homeState,
            )
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                HomeUiState(),
            )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
