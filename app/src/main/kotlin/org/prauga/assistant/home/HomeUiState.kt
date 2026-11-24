// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.home

import org.prauga.assistant.capabilities.Capability
import org.prauga.assistant.capabilities.CapabilityMap
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.core.ConversationSummary
import org.prauga.assistant.platform.binder.ProviderComponent

data class HomeUiState(
    val scanning: Boolean = true,
    val capabilities: List<CapabilityTile> = emptyList(),
    val connected: Int = 0,
    val supported: Int = 0,
    val recent: List<ConversationSummary> = emptyList(),
)

data class CapabilityTile(
    val capability: Capability,
    val apps: List<String>,
    val defaultApp: String?,
)

internal fun homeState(
    map: CapabilityMap,
    supported: List<ProviderComponent>,
    grants: Map<String, ContextGrant>,
    recent: List<ConversationSummary>,
): HomeUiState =
    HomeUiState(
        scanning = map === CapabilityMap.Empty,
        capabilities =
            map.available.map { capability ->
                CapabilityTile(
                    capability,
                    map.appsFor(capability).map { it.label },
                    map.defaultFor(capability)?.label,
                )
            },
        connected = supported.count { grants[it.packageName]?.enabled == true },
        supported = supported.size,
        recent = recent.take(RECENT_COUNT),
    )

private const val RECENT_COUNT = 3
