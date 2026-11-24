// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.ContentUris
import android.content.Context
import android.provider.CalendarContract.Instances

/** One occurrence of a calendar event. */
data class CalendarEvent(val title: String, val begin: Long, val end: Long, val allDay: Boolean)

/** Reads the phone's own calendars; needs READ_CALENDAR. */
fun interface CalendarEvents {
    /** Occurrences overlapping [from, to), earliest first. */
    fun between(from: Long, to: Long): List<CalendarEvent>
}

class AndroidCalendarEvents(private val context: Context) : CalendarEvents {
    override fun between(from: Long, to: Long): List<CalendarEvent> {
        val uri =
            Instances.CONTENT_URI.buildUpon()
                .also {
                    ContentUris.appendId(it, from)
                    ContentUris.appendId(it, to)
                }
                .build()
        val found = ArrayList<CalendarEvent>()
        context.contentResolver.query(uri, PROJECTION, VISIBLE, null, ORDER)?.use { cursor ->
            while (cursor.moveToNext()) {
                found +=
                    CalendarEvent(
                        cursor.getString(0).orEmpty(),
                        cursor.getLong(1),
                        cursor.getLong(2),
                        cursor.getInt(3) != 0,
                    )
            }
        }
        return found
    }

    private companion object {
        val PROJECTION = arrayOf(Instances.TITLE, Instances.BEGIN, Instances.END, Instances.ALL_DAY)
        const val VISIBLE = "${Instances.VISIBLE} = 1"
        const val ORDER = "${Instances.BEGIN} ASC"
    }
}
