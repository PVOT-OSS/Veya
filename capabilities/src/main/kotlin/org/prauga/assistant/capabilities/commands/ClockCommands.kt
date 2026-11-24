// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.Context
import android.content.Intent
import android.icu.text.MeasureFormat
import android.icu.util.Measure
import android.icu.util.MeasureUnit
import android.provider.AlarmClock
import android.text.format.DateFormat
import java.time.Duration
import java.time.ZonedDateTime
import java.util.Date
import org.prauga.assistant.capabilities.R
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.understanding.Durations
import org.prauga.assistant.understanding.Lexicon
import org.prauga.assistant.understanding.Times

class TimerCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(
        context,
        router,
        lexicon,
        ID,
        R.string.command_timer,
        question = R.string.command_timer_question,
    ) {
    private val durations = Durations(lexicon)

    override val unclear = R.string.command_timer_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val length = subject?.let(durations::parse) ?: return null
        val seconds = length.seconds
        if (seconds < 1) return null
        if (seconds > MAX_SECONDS)
            return Refusal(context.getString(R.string.command_timer_too_long))
        val intent =
            Intent(AlarmClock.ACTION_SET_TIMER)
                .putExtra(AlarmClock.EXTRA_LENGTH, seconds.toInt())
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return Request(intent) { app ->
            context.getString(R.string.command_timer_done, describe(length), app)
        }
    }

    private fun describe(length: Duration): String {
        val total = length.seconds
        val hours = total / SECONDS_PER_HOUR
        val minutes = total % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
        val seconds = total % SECONDS_PER_MINUTE
        val parts =
            listOfNotNull(
                Measure(hours, MeasureUnit.HOUR).takeIf { hours > 0 },
                Measure(minutes, MeasureUnit.MINUTE).takeIf { minutes > 0 },
                Measure(seconds, MeasureUnit.SECOND).takeIf { seconds > 0 },
            )
        val locale = context.resources.configuration.locales[0]
        return MeasureFormat.getInstance(locale, MeasureFormat.FormatWidth.WIDE)
            .formatMeasures(*parts.toTypedArray())
    }

    companion object {
        const val ID = "command.timer"
        private const val MAX_SECONDS = 86_400L
        private const val SECONDS_PER_HOUR = 3_600L
        private const val SECONDS_PER_MINUTE = 60L
    }
}

class AlarmCommand(
    context: Context,
    router: AppRouter,
    lexicon: Lexicon,
    private val now: () -> ZonedDateTime = ZonedDateTime::now,
) :
    CapabilityCommand(
        context,
        router,
        lexicon,
        ID,
        R.string.command_alarm,
        question = R.string.command_alarm_question,
    ) {
    private val times = Times(lexicon)

    override val unclear = R.string.command_alarm_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val current = now()
        val at = subject?.let { times.parse(it, current) }?.at ?: return null
        if (at.isAfter(current.plusDays(1)))
            return Refusal(context.getString(R.string.command_alarm_too_far))
        val intent =
            Intent(AlarmClock.ACTION_SET_ALARM)
                .putExtra(AlarmClock.EXTRA_HOUR, at.hour)
                .putExtra(AlarmClock.EXTRA_MINUTES, at.minute)
                .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        val time = DateFormat.getTimeFormat(context).format(Date.from(at.toInstant()))
        return Request(intent) { app -> context.getString(R.string.command_alarm_done, time, app) }
    }

    companion object {
        const val ID = "command.alarm"
    }
}

class ShowAlarmsCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(context, router, lexicon, ID, R.string.command_alarms) {
    override val unclear = R.string.command_alarms_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? =
        if (subject != null) null
        else
            Request(Intent(AlarmClock.ACTION_SHOW_ALARMS)) { app ->
                context.getString(R.string.command_alarms_done, app)
            }

    companion object {
        const val ID = "command.alarms"
    }
}
