// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import android.text.format.DateFormat
import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Lexicon

class TimeHandler(
    private val context: Context,
    lexicon: Lexicon,
    private val clock: Clock = Clock.systemDefaultZone(),
) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_time))

    private val matcher = PatternMatcher.from(ID, lexicon.phrases(ID))

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override fun closeness(utterance: Utterance): Float = matcher.closeness(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val pattern =
            DateFormat.getBestDateTimePattern(
                context.resources.configuration.locales[0],
                skeleton(),
            )
        val time = ZonedDateTime.now(clock).format(DateTimeFormatter.ofPattern(pattern))
        return IntentOutcome.Reply(context.getString(R.string.time_now, time))
    }

    private fun skeleton(): String = if (DateFormat.is24HourFormat(context)) "Hm" else "hm"

    companion object {
        const val ID = "time.now"
    }
}
