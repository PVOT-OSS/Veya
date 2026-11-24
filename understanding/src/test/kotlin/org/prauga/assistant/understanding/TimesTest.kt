// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.ZoneOffset
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimesTest {
    private val times = Times(Lexicon.english())
    private val now = ZonedDateTime.of(2026, 11, 24, 10, 15, 0, 0, ZoneOffset.UTC)

    private fun at(day: Int, hour: Int, minute: Int = 0) =
        ZonedDateTime.of(2026, 11, day, hour, minute, 0, 0, ZoneOffset.UTC)

    private fun check(text: String, expected: ZonedDateTime, dateGiven: Boolean) {
        val parsed = times.parse(text, now)
        assertEquals(text, expected, parsed?.at)
        assertEquals(text, dateGiven, parsed?.dateGiven)
    }

    @Test
    fun clockTimesWithAndWithoutMeridiem() {
        check("7am", at(25, 7), false)
        check("7 am", at(25, 7), false)
        check("7:30 pm", at(24, 19, 30), false)
        check("7:30pm", at(24, 19, 30), false)
        check("19:00", at(24, 19), false)
        check("11", at(24, 11), false)
        check("9", at(24, 21), false)
        check("10:20", at(24, 10, 20), false)
        check("10:10", at(24, 22, 10), false)
        check("at 6 in the evening", at(24, 18), false)
        check("7 in the morning", at(25, 7), false)
        check("noon", at(24, 12), false)
        check("midnight", at(25, 0), false)
        check("12am", at(25, 0), false)
        check("12pm", at(24, 12), false)
        check("half past 7", at(24, 19, 30), false)
        check("quarter to 8", at(24, 19, 45), false)
        check("quarter past seven", at(24, 19, 15), false)
        check("seven", at(24, 19), false)
        check("8 o'clock", at(24, 20), false)
    }

    @Test
    fun daysAndTimes() {
        check("tomorrow at 5", at(25, 17), true)
        check("tomorrow at 7", at(25, 7), true)
        check("tomorrow 7am", at(25, 7), true)
        check("5pm tomorrow", at(25, 17), true)
        check("tonight at 8", at(24, 20), true)
        check("today at 3pm", at(24, 15), true)
        check("on friday at 9", at(27, 9), true)
        check("monday at 9am", at(30, 9), true)
        check("next friday 8am", at(27, 8), true)
        check("tuesday at noon", at(24, 12), true)
        check("day after tomorrow at 10", at(26, 10), true)
        check("tomorrow", at(25, 9), true)
        check("tomorrow at midnight", at(25, 0), true)
    }

    @Test
    fun relativeTimes() {
        check("in 10 minutes", at(24, 10, 25), false)
        check("in an hour and a half", at(24, 11, 45), false)
        check("in 2 days", at(26, 10, 15), false)
    }

    @Test
    fun rejectsWhatIsNotATime() {
        listOf(
                "",
                "pasta",
                "7 apples",
                "25:00",
                "7:75",
                "13am",
                "in a while",
                "tomorrow at the cinema",
                "at 5 and 6",
            )
            .forEach { assertNull(it, times.parse(it, now)) }
    }
}
