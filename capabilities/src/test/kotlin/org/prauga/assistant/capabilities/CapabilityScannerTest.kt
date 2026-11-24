// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CapabilityScannerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val packages = shadowOf(context.packageManager)

    private fun app(
        packageName: String,
        label: String,
        category: Int = ApplicationInfo.CATEGORY_UNDEFINED,
    ): ApplicationInfo {
        val info =
            ApplicationInfo().apply {
                this.packageName = packageName
                nonLocalizedLabel = label
                this.category = category
            }
        packages.installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo = info
            }
        )
        return info
    }

    private fun handles(info: ApplicationInfo, capability: Capability) {
        val resolve =
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = info.packageName
                        name = "${info.packageName}.Main"
                        applicationInfo = info
                    }
            }
        packages.addResolveInfoForIntent(capability.probe(), resolve)
    }

    private fun scanner() = CapabilityScanner(context.packageManager, "org.prauga.assistant.dev")

    @Test
    fun findsWhatEachAppCanDo() {
        val maps = app("app.organicmaps", "Organic Maps", ApplicationInfo.CATEGORY_MAPS)
        val clock = app("org.prauga.clock", "Clock")
        handles(maps, Capability.MAP)
        handles(clock, Capability.SET_ALARM)
        handles(clock, Capability.SET_TIMER)
        val map = scanner().scan()
        assertEquals(listOf("Clock", "Organic Maps"), map.apps.map { it.label })
        assertEquals(
            setOf(Capability.SET_ALARM, Capability.SET_TIMER),
            map.apps.first().capabilities,
        )
        assertEquals(AppKind.MAPS, map.appsFor(Capability.MAP).single().kind)
        assertEquals(
            listOf(Capability.MAP, Capability.SET_ALARM, Capability.SET_TIMER),
            map.available,
        )
    }

    @Test
    fun reportsTheDefaultAppWhenOneIsSet() {
        val maps = app("app.organicmaps", "Organic Maps")
        handles(maps, Capability.MAP)
        assertEquals("app.organicmaps", scanner().scan().defaultFor(Capability.MAP)?.packageName)
    }

    @Test
    fun theSystemChooserIsNotADefault() {
        val chooser = app("android", "Android System")
        packages.addResolveInfoForIntent(
            Capability.MAP.probe(),
            ResolveInfo().apply {
                activityInfo =
                    ActivityInfo().apply {
                        packageName = "android"
                        name = "com.android.internal.app.ResolverActivity"
                        applicationInfo = chooser
                    }
            },
        )
        handles(app("app.organicmaps", "Organic Maps"), Capability.MAP)
        val map = scanner().scan()
        assertNull(map.defaultFor(Capability.MAP))
        assertTrue(map.appsFor(Capability.MAP).any { it.packageName == "app.organicmaps" })
    }

    @Test
    fun leavesVeyaItselfOut() {
        val veya = app("org.prauga.assistant.dev", "Veya")
        handles(veya, Capability.APP_SEARCH)
        assertTrue(scanner().scan().apps.isEmpty())
        assertNull(scanner().scan().defaultFor(Capability.APP_SEARCH))
    }

    @Test
    fun everyContractHasADistinctProbe() {
        val probes = Capability.entries.map { it.probe() }
        assertEquals(
            probes.size,
            probes.distinctBy { listOf(it.action, it.dataString, it.type, it.categories) }.size,
        )
    }
}
