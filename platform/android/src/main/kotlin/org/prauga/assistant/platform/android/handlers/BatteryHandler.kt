// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Lexicon

class BatteryHandler(private val context: Context, lexicon: Lexicon) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_battery))

    private val matcher = PatternMatcher.from(ID, lexicon.phrases(ID))

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val status = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = status?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = status?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level < 0 || scale <= 0)
            return IntentOutcome.Failed(context.getString(R.string.battery_unknown))
        val percent = level * 100 / scale
        val state = status?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val text =
            when (state) {
                BatteryManager.BATTERY_STATUS_FULL -> context.getString(R.string.battery_level_full)
                BatteryManager.BATTERY_STATUS_CHARGING ->
                    context.getString(R.string.battery_level_charging, percent)
                else -> context.getString(R.string.battery_level, percent)
            }
        return IntentOutcome.Reply(text)
    }

    companion object {
        const val ID = "device.battery"
    }
}
