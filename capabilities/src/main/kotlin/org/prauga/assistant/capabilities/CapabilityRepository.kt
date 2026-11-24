// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CapabilityRepository(
    private val scanner: () -> CapabilityMap,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
) {
    private val state = MutableStateFlow(CapabilityMap.Empty)

    val map: StateFlow<CapabilityMap> = state.asStateFlow()

    fun refresh() {
        scope.launch { state.value = withContext(io) { scanner() } }
    }
}
