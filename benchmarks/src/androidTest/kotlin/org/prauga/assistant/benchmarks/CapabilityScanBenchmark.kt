// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.benchmarks

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.capabilities.CapabilityScanner

@RunWith(AndroidJUnit4::class)
class CapabilityScanBenchmark {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scanner = CapabilityScanner(context.packageManager, context.packageName)

    @Test
    fun fullScan() {
        Latency.measure("capabilities.fullScan", iterations = SCANS) { scanner.scan() }
    }

    private companion object {
        const val SCANS = 200
    }
}
