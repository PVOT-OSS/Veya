// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android

import android.content.Context
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.platform.android.handlers.BatteryHandler
import org.prauga.assistant.platform.android.handlers.CalculateHandler
import org.prauga.assistant.platform.android.handlers.ConvertHandler
import org.prauga.assistant.platform.android.handlers.DateHandler
import org.prauga.assistant.platform.android.handlers.DeviceInfoHandler
import org.prauga.assistant.platform.android.handlers.OpenAppHandler
import org.prauga.assistant.platform.android.handlers.OpenSettingsHandler
import org.prauga.assistant.platform.android.handlers.TimeHandler
import org.prauga.assistant.platform.android.handlers.TorchHandler
import org.prauga.assistant.understanding.Lexicon

object DeviceHandlers {
    fun create(context: Context, lexicon: Lexicon): List<IntentHandler> {
        val launcher = ActivityLauncher(context)
        return listOf(
            BatteryHandler(context, lexicon),
            TorchHandler(context, lexicon, on = true),
            TorchHandler(context, lexicon, on = false),
            OpenAppHandler(context, launcher, lexicon),
            OpenSettingsHandler(context, launcher, lexicon),
            TimeHandler(context, lexicon),
            DateHandler(context, lexicon),
            DeviceInfoHandler(context, lexicon),
            CalculateHandler(context, lexicon),
            ConvertHandler(context, lexicon),
        )
    }
}
