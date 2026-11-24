// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.platform.binder.IntegrationGrants
import org.prauga.assistant.platform.binder.LiveProviders

@HiltViewModel
class IntegrationsViewModel
@Inject
constructor(private val live: LiveProviders, private val grants: IntegrationGrants) : ViewModel() {
    val state: StateFlow<IntegrationsUiState> =
        combine(live.available, grants.all) { apps, granted ->
                IntegrationsUiState(
                    apps.map { app ->
                        val grant = granted[app.packageName] ?: ContextGrant.Denied
                        IntegrationRow(
                            app.packageName,
                            app.label,
                            grant.enabled,
                            grant.maxSensitivity,
                        )
                    }
                )
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                IntegrationsUiState(),
            )

    fun onEvent(event: IntegrationsEvent) {
        when (event) {
            is IntegrationsEvent.SetEnabled -> {
                val current = grants.grant(event.packageName)
                val limit =
                    if (event.enabled) maxOf(current.maxSensitivity, Sensitivity.PERSONAL)
                    else current.maxSensitivity
                grants.set(event.packageName, ContextGrant(event.enabled, limit))
                if (event.enabled) live.warmUp()
            }
            is IntegrationsEvent.SetLimit -> {
                val current = grants.grant(event.packageName)
                grants.set(event.packageName, current.copy(maxSensitivity = event.limit))
                live.invalidate(event.packageName)
            }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
