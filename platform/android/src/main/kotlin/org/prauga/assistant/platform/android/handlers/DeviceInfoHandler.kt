// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import android.os.Build
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.PatternMatcher.Companion.rule
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R

class DeviceInfoHandler(private val context: Context) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_device_info))

    private val matcher =
        PatternMatcher(
            ID,
            listOf(
                rule("(show )?(my )?(device|phone) (info|information|details)", 0.95f),
                rule("what (phone|device) is this", 0.95f),
                rule("((what|which) )?android version( is this| am i on| do i have)?", 0.9f),
            ),
        )

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome =
        IntentOutcome.Reply(
            context.getString(
                R.string.device_info,
                Build.MANUFACTURER.replaceFirstChar { it.titlecase() },
                Build.MODEL,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
            )
        )

    companion object {
        const val ID = "device.info"
    }
}
