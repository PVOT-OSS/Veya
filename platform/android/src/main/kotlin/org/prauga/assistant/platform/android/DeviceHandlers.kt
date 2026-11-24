// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android

import android.content.Context
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.platform.android.handlers.BatteryHandler
import org.prauga.assistant.platform.android.handlers.DateHandler
import org.prauga.assistant.platform.android.handlers.DeviceInfoHandler
import org.prauga.assistant.platform.android.handlers.OpenAppHandler
import org.prauga.assistant.platform.android.handlers.OpenSettingsHandler
import org.prauga.assistant.platform.android.handlers.TimeHandler
import org.prauga.assistant.platform.android.handlers.TorchHandler

object DeviceHandlers {
    fun create(context: Context): List<IntentHandler> {
        val launcher = ActivityLauncher(context)
        return listOf(
            BatteryHandler(context),
            TorchHandler(context, on = true),
            TorchHandler(context, on = false),
            OpenAppHandler(context, launcher),
            OpenSettingsHandler(context, launcher),
            TimeHandler(context),
            DateHandler(context),
            DeviceInfoHandler(context),
        )
    }
}
