// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DatesTest {
    private val dates = Dates(Lexicon.english())
    private val now = ZonedDateTime.of(2026, 11, 24, 10, 15, 0, 0, ZoneOffset.UTC)

    private fun date(text: String) = dates.parse(text, now)

    @Test
    fun readsNamedAndWrittenDays() {
        assertEquals(LocalDate.of(2026, 12, 25), date("christmas"))
        assertEquals(LocalDate.of(2027, 1, 1), date("new year's day"))
        assertEquals(LocalDate.of(2027, 10, 31), date("halloween"))
        assertEquals(LocalDate.of(2026, 12, 25), date("25 december"))
        assertEquals(LocalDate.of(2026, 12, 25), date("the 25th of december"))
        assertEquals(LocalDate.of(2026, 12, 25), date("Dec 25th"))
        assertEquals(LocalDate.of(2027, 3, 1), date("march 1"))
        assertEquals(LocalDate.of(2028, 2, 29), date("29 feb 2028"))
        assertEquals(LocalDate.of(2026, 11, 24), date("24 november"))
    }

    @Test
    fun readsDaysCountedFromToday() {
        assertEquals(LocalDate.of(2026, 12, 15), date("3 weeks from now"))
        assertEquals(LocalDate.of(2026, 12, 15), date("in three weeks"))
        assertEquals(LocalDate.of(2026, 11, 14), date("10 days ago"))
        assertEquals(LocalDate.of(2026, 12, 8), date("in a fortnight"))
        assertEquals(LocalDate.of(2027, 5, 24), date("6 months from today"))
        assertEquals(LocalDate.of(2026, 11, 25), date("tomorrow"))
        assertEquals(LocalDate.of(2026, 11, 27), date("next friday"))
    }

    @Test
    fun refusesWhatIsNotADay() {
        listOf("dinner", "30 february", "3 apples ago", "my next meeting", "25", "").forEach {
            assertNull(it, date(it))
        }
    }

    @Test
    fun monthNamesComeFromThePlatform() {
        val german = Dates(Lexicon.load(listOf(Locale.GERMAN)))
        assertEquals(LocalDate.of(2026, 12, 25), german.parse("25 dezember", now))
    }
}
