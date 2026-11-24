// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.Sensitivity
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class IntegrationGrantsTest {
    @Test
    fun appsAreOffUntilEnabledAndChoicesPersist() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val grants = IntegrationGrants(context)
        assertFalse(grants.isEnabled("com.example.notes"))
        grants.set("com.example.notes", ContextGrant(true, Sensitivity.PERSONAL))
        assertTrue(grants.isEnabled("com.example.notes"))
        val reloaded = IntegrationGrants(context)
        assertEquals(ContextGrant(true, Sensitivity.PERSONAL), reloaded.grant("com.example.notes"))
        assertEquals(setOf("com.example.notes"), reloaded.all.value.keys)
    }
}
