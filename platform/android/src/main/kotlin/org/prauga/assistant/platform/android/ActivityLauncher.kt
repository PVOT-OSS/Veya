// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

class ActivityLauncher(private val context: Context) {
    fun launch(intent: Intent): Boolean {
        val resolvable = intent.resolveActivity(context.packageManager) != null
        if (!resolvable) return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
