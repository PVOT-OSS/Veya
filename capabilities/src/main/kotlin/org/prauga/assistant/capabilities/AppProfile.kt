// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities

enum class AppKind {
    GAME,
    AUDIO,
    VIDEO,
    IMAGE,
    SOCIAL,
    NEWS,
    MAPS,
    PRODUCTIVITY,
    ACCESSIBILITY,
    UNDECLARED,
}

data class AppProfile(
    val packageName: String,
    val label: String,
    val kind: AppKind,
    val capabilities: Set<Capability>,
)

data class CapabilityMap(val apps: List<AppProfile>, val defaults: Map<Capability, String>) {
    private val byCapability: Map<Capability, List<AppProfile>> =
        Capability.entries.associateWith { capability ->
            apps.filter { capability in it.capabilities }
        }

    fun appsFor(capability: Capability): List<AppProfile> = byCapability[capability].orEmpty()

    fun defaultFor(capability: Capability): AppProfile? =
        defaults[capability]?.let { packageName ->
            apps.firstOrNull { it.packageName == packageName }
        }

    val available: List<Capability>
        get() = Capability.entries.filter { appsFor(it).isNotEmpty() }

    companion object {
        val Empty = CapabilityMap(emptyList(), emptyMap())
    }
}
