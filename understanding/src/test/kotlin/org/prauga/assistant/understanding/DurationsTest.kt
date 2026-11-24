// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationsTest {
    private val durations = Durations(Lexicon.english())

    @Test
    fun readsCommonDurations() {
        val cases =
            mapOf(
                "10 minutes" to 600L,
                "10 mins" to 600L,
                "1 minute" to 60L,
                "a minute" to 60L,
                "an hour" to 3_600L,
                "90 seconds" to 90L,
                "90 secs" to 90L,
                "2 hours" to 7_200L,
                "1.5 hours" to 5_400L,
                "1 hour 30 minutes" to 5_400L,
                "1 hour and 30 minutes" to 5_400L,
                "an hour and a half" to 5_400L,
                "half an hour" to 1_800L,
                "quarter of an hour" to 900L,
                "a quarter hour" to 900L,
                "2h" to 7_200L,
                "5m" to 300L,
                "2h 5m" to 7_500L,
                "twenty five minutes" to 1_500L,
                "twenty-five minutes" to 1_500L,
                "3 days" to 259_200L,
                "10 minutes and 30 seconds" to 630L,
                "for 5 minutes" to 300L,
                "a couple minutes" to 120L,
                "ten seconds" to 10L,
            )
        cases.forEach { (text, seconds) ->
            assertEquals(text, Duration.ofSeconds(seconds), durations.parse(text))
        }
    }

    @Test
    fun rejectsWhatIsNotADuration() {
        listOf(
                "",
                "minutes",
                "10",
                "10 apples",
                "for the pasta",
                "half",
                "0 minutes",
                "ten minutes ago please",
            )
            .forEach { assertNull(it, durations.parse(it)) }
    }
}
