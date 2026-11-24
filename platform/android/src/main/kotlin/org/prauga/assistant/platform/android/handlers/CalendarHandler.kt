// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.Manifest
import android.content.Context
import android.text.format.DateUtils
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Dates
import org.prauga.assistant.understanding.Lexicon

/**
 * Reads the phone's calendars: the next event ([next]) or one day's events ("what's on my calendar
 * tomorrow"). Nothing leaves the phone.
 */
class CalendarHandler(
    private val context: Context,
    lexicon: Lexicon,
    private val events: CalendarEvents,
    private val next: Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) : IntentHandler {
    private val id = if (next) NEXT_ID else DAY_ID

    override val descriptor =
        IntentDescriptor(
            id,
            context.getString(
                if (next) R.string.intent_calendar_next else R.string.intent_calendar_day
            ),
            requiredPermissions = setOf(Manifest.permission.READ_CALENDAR),
        )

    private val matcher = PatternMatcher.from(id, lexicon.phrases(id))
    private val dates = Dates(lexicon)

    override fun match(utterance: Utterance): IntentMatch? {
        val found = matcher.match(utterance) ?: return null
        val subject = found.entities[SUBJECT] ?: return found
        return found.takeIf { dates.parse(subject, ZonedDateTime.now(clock)) != null }
    }

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val now = ZonedDateTime.now(clock)
        return if (next) nextEvent(now) else day(now, match.entities[SUBJECT])
    }

    private fun nextEvent(now: ZonedDateTime): IntentOutcome {
        val from = now.toInstant().toEpochMilli()
        val upcoming =
            events.between(from, from + TimeUnit.DAYS.toMillis(LOOK_AHEAD_DAYS)).firstOrNull {
                it.begin >= from && !it.allDay
            } ?: return IntentOutcome.Reply(context.getString(R.string.calendar_next_none))
        val whenText = DateUtils.formatDateTime(context, upcoming.begin, NEXT_FORMAT)
        return IntentOutcome.Reply(
            context.getString(R.string.calendar_next, title(upcoming), whenText)
        )
    }

    private fun day(now: ZonedDateTime, subject: String?): IntentOutcome {
        val date = subject?.let { dates.parse(it, now) } ?: now.toLocalDate()
        val start = date.atStartOfDay(now.zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(now.zone).toInstant().toEpochMilli()
        val dayText = dayName(date, now.toLocalDate(), start)
        val found = events.between(start, end)
        if (found.isEmpty())
            return IntentOutcome.Reply(context.getString(R.string.calendar_day_none, dayText))
        val listed =
            found.take(MAX_LISTED).joinToString(", ") { event ->
                if (event.allDay) title(event)
                else
                    context.getString(
                        R.string.calendar_day_item,
                        DateUtils.formatDateTime(context, event.begin, DateUtils.FORMAT_SHOW_TIME),
                        title(event),
                    )
            }
        return IntentOutcome.Reply(
            context.resources.getQuantityString(
                R.plurals.calendar_day,
                found.size,
                dayText,
                found.size,
                listed,
            )
        )
    }

    private fun dayName(date: LocalDate, today: LocalDate, start: Long): String =
        when (date) {
            today -> context.getString(R.string.calendar_today)
            today.plusDays(1) -> context.getString(R.string.calendar_tomorrow)
            else ->
                context.getString(
                    R.string.calendar_on,
                    DateUtils.formatDateTime(context, start, DAY_FORMAT),
                )
        }

    private fun title(event: CalendarEvent): String =
        event.title.ifBlank { context.getString(R.string.calendar_untitled) }

    companion object {
        const val NEXT_ID = "calendar.next"
        const val DAY_ID = "calendar.day"
        private const val SUBJECT = "subject"
        private const val LOOK_AHEAD_DAYS = 60L
        private const val MAX_LISTED = 5
        private const val NEXT_FORMAT =
            DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_ABBREV_ALL
        private const val DAY_FORMAT =
            DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_ABBREV_MONTH
    }
}
