// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.prauga.assistant.capabilities.AppKind
import org.prauga.assistant.capabilities.AppProfile
import org.prauga.assistant.capabilities.Capability
import org.prauga.assistant.capabilities.CapabilityMap
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.core.ConversationSummary
import org.prauga.assistant.platform.binder.ProviderComponent

class HomeStateTest {
    private val maps =
        AppProfile("app.organicmaps", "Organic Maps", AppKind.MAPS, setOf(Capability.MAP))
    private val clock =
        AppProfile(
            "org.prauga.clock",
            "Clock",
            AppKind.UNDECLARED,
            setOf(Capability.SET_ALARM, Capability.SET_TIMER),
        )
    private val osm = AppProfile("net.osmand", "OsmAnd", AppKind.MAPS, setOf(Capability.MAP))

    @Test
    fun saysItIsStillScanningBeforeTheFirstScan() {
        assertTrue(homeState(CapabilityMap.Empty, emptyList(), emptyMap(), emptyList()).scanning)
        assertFalse(
            homeState(CapabilityMap(emptyList(), emptyMap()), emptyList(), emptyMap(), emptyList())
                .scanning
        )
    }

    @Test
    fun showsOneTilePerCapabilityWithItsAppsAndDefault() {
        val map =
            CapabilityMap(
                listOf(clock, maps, osm),
                mapOf(Capability.SET_ALARM to "org.prauga.clock"),
            )
        val tiles = homeState(map, emptyList(), emptyMap(), emptyList()).capabilities
        assertEquals(
            listOf(Capability.MAP, Capability.SET_ALARM, Capability.SET_TIMER),
            tiles.map { it.capability },
        )
        assertEquals(listOf("Organic Maps", "OsmAnd"), tiles.first().apps)
        assertEquals(null, tiles.first().defaultApp)
        assertEquals("Clock", tiles[1].defaultApp)
    }

    @Test
    fun countsAppsSharingWithVeyaAndKeepsThreeRecentQuestions() {
        val supported =
            listOf(
                ProviderComponent("org.prauga.messages", "S", "Messages"),
                ProviderComponent("org.prauga.notes", "S", "Notes"),
            )
        val grants =
            mapOf(
                "org.prauga.messages" to ContextGrant(true, Sensitivity.PERSONAL),
                "org.prauga.notes" to ContextGrant.Denied,
            )
        val recent = (1..5).map { ConversationSummary("c$it", "q$it", it.toLong()) }
        val state = homeState(CapabilityMap.Empty, supported, grants, recent)
        assertEquals(1, state.connected)
        assertEquals(2, state.supported)
        assertEquals(listOf("c1", "c2", "c3"), state.recent.map { it.id })
    }
}
