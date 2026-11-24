// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.prauga.assistant.platform.android.handlers.AppMatcher
import org.prauga.assistant.platform.android.handlers.LaunchableApp

class AppMatcherTest {
    private val apps =
        listOf(
            LaunchableApp("Camera", "org.lineageos.aperture"),
            LaunchableApp("Camera Probe", "org.prauga.cameraprobe"),
            LaunchableApp("Google Maps", "com.google.android.apps.maps"),
            LaunchableApp("Messages", "org.prauga.messages"),
        )

    @Test
    fun prefersExactLabels() {
        assertEquals("org.lineageos.aperture", AppMatcher.best("camera", apps)?.packageName)
    }

    @Test
    fun matchesWholeWordsInLabels() {
        assertEquals("com.google.android.apps.maps", AppMatcher.best("maps", apps)?.packageName)
    }

    @Test
    fun ignoresCaseAndPunctuation() {
        assertEquals("org.prauga.messages", AppMatcher.best("MESSAGES!", apps)?.packageName)
    }

    @Test
    fun returnsNullWhenNothingMatches() {
        assertNull(AppMatcher.best("calculator", apps))
        assertNull(AppMatcher.best("  ", apps))
    }

    @Test
    fun wholeWordsNeverMatchPartOfAWord() {
        assertEquals("org.prauga.messages", AppMatcher.best("mess", apps)?.packageName)
        assertNull(AppMatcher.best("mess", apps, wholeWords = true))
        assertEquals(
            "com.google.android.apps.maps",
            AppMatcher.best("google maps", apps, wholeWords = true)?.packageName,
        )
        assertEquals(
            "org.prauga.cameraprobe",
            AppMatcher.best("probe", apps, wholeWords = true)?.packageName,
        )
    }
}
