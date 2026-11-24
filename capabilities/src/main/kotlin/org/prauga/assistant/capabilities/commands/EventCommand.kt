// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.text.format.DateUtils
import java.time.ZonedDateTime
import org.prauga.assistant.capabilities.R
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.understanding.Lexicon
import org.prauga.assistant.understanding.ParsedTime
import org.prauga.assistant.understanding.Times

/** "Dinner with Sam tomorrow at 8": opens the calendar app's editor with title and time filled. */
class EventCommand(
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
        R.string.command_event,
        question = R.string.command_event_question,
    ) {
    private val times = Times(lexicon)

    override val unclear = R.string.command_event_unclear

    override fun understands(subject: String?): Boolean =
        subject == null || split(subject, now()) != null

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val (title, time) = subject?.let { split(it, now()) } ?: return null
        val begin = time.at.toInstant().toEpochMilli()
        val intent =
            Intent(Intent.ACTION_INSERT)
                .setDataAndType(CalendarContract.Events.CONTENT_URI, EVENT_TYPE)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, begin + HOUR_MILLIS)
        val whenText = DateUtils.formatDateTime(context, begin, WHEN_FORMAT)
        return Request(intent) { app ->
            context.getString(R.string.command_event_done, title, whenText, app)
        }
    }

    /** The title before the time; the time takes as many trailing words as still parse. */
    private fun split(subject: String, current: ZonedDateTime): Pair<String, ParsedTime>? {
        var space = subject.indexOf(' ')
        while (space > 0) {
            val rest = subject.substring(space + 1)
            if (times.begins(rest.substringBefore(' '))) {
                val title = subject.substring(0, space).trim()
                val time = times.parse(rest, current)
                if (time != null && title.isNotEmpty()) return title to time
            }
            space = subject.indexOf(' ', space + 1)
        }
        return null
    }

    companion object {
        const val ID = "command.event"
        private const val EVENT_TYPE = "vnd.android.cursor.dir/event"
        private const val HOUR_MILLIS = 3_600_000L
        private const val WHEN_FORMAT =
            DateUtils.FORMAT_SHOW_WEEKDAY or
                DateUtils.FORMAT_SHOW_DATE or
                DateUtils.FORMAT_SHOW_TIME or
                DateUtils.FORMAT_ABBREV_ALL
    }
}
