// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

data class ParsedTime(val at: ZonedDateTime, val dateGiven: Boolean)

object Times {
    private val CLOCK = Regex("(\\d{1,2})(?::(\\d{2}))?(am|pm|a\\.m\\.|p\\.m\\.)?")
    private val DAYS =
        mapOf(
            "monday" to DayOfWeek.MONDAY,
            "mon" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "tue" to DayOfWeek.TUESDAY,
            "tues" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "wed" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "thu" to DayOfWeek.THURSDAY,
            "thurs" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "fri" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "sat" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY,
            "sun" to DayOfWeek.SUNDAY,
        )
    private val FILLER = setOf("at", "on", "for", "by", "the", "this", "o'clock", "oclock")
    private const val NOON = 12
    private const val MORNING_FROM = 7
    private const val MINUTES_IN_HOUR = 60
    private const val HALF_HOUR = 30
    private const val QUARTER_HOUR = 15
    private const val HOURS_IN_HALF_DAY = 12

    private enum class Meridiem {
        AM,
        PM,
    }

    private class Clock(val hour: Int, val minute: Int, val meridiem: Meridiem?)

    fun parse(text: String, now: ZonedDateTime): ParsedTime? {
        val tokens = Tokens.of(text)
        if (tokens.isEmpty()) return null
        if (tokens.first() == "in") {
            val duration = Durations.parse(tokens.drop(1).joinToString(" ")) ?: return null
            return ParsedTime(now.plus(duration).truncatedTo(ChronoUnit.SECONDS), dateGiven = false)
        }
        var date: LocalDate? = null
        var clock: Clock? = null
        var hint: Meridiem? = null
        var index = 0
        while (index < tokens.size) {
            val word = tokens[index]
            when {
                word in FILLER -> index++
                word == "today" -> {
                    date = now.toLocalDate()
                    index++
                }
                word == "tonight" -> {
                    date = now.toLocalDate()
                    hint = Meridiem.PM
                    index++
                }
                word == "tomorrow" -> {
                    date = now.toLocalDate().plusDays(1)
                    index++
                }
                word == "day" &&
                    tokens.getOrNull(index + 1) == "after" &&
                    tokens.getOrNull(index + 2) == "tomorrow" -> {
                    date = now.toLocalDate().plusDays(2)
                    index += 3
                }
                word == "next" && DAYS.containsKey(tokens.getOrNull(index + 1)) -> {
                    val day = DAYS.getValue(tokens[index + 1])
                    date = now.toLocalDate().with(TemporalAdjusters.next(day))
                    index += 2
                }
                DAYS.containsKey(word) -> {
                    date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(DAYS.getValue(word)))
                    index++
                }
                word == "noon" || word == "midday" -> {
                    clock = Clock(NOON, 0, Meridiem.PM)
                    index++
                }
                word == "midnight" -> {
                    clock = Clock(0, 0, Meridiem.AM)
                    index++
                }
                word == "morning" -> {
                    hint = Meridiem.AM
                    index++
                }
                word == "afternoon" || word == "evening" || word == "night" -> {
                    hint = Meridiem.PM
                    index++
                }
                word == "in" && tokens.getOrNull(index + 1) == "the" -> index += 2
                word == "am" || word == "a.m." -> {
                    hint = Meridiem.AM
                    index++
                }
                word == "pm" || word == "p.m." -> {
                    hint = Meridiem.PM
                    index++
                }
                (word == "half" || word == "quarter") &&
                    tokens.getOrNull(index + 1) in setOf("past", "to") -> {
                    val hour = hourWord(tokens.getOrNull(index + 2)) ?: return null
                    val offset = if (word == "half") HALF_HOUR else QUARTER_HOUR
                    clock =
                        if (tokens[index + 1] == "past") Clock(hour, offset, null)
                        else
                            Clock(
                                (hour + HOURS_IN_HALF_DAY - 1) % HOURS_IN_HALF_DAY,
                                MINUTES_IN_HOUR - offset,
                                null,
                            )
                    index += 3
                }
                else -> {
                    if (clock != null) return null
                    val match = CLOCK.matchEntire(word)
                    clock =
                        if (match != null) {
                            val meridiem =
                                match.groupValues[3]
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { if (it.startsWith("a")) Meridiem.AM else Meridiem.PM }
                            Clock(
                                match.groupValues[1].toInt(),
                                match.groupValues[2].ifEmpty { "0" }.toInt(),
                                meridiem,
                            )
                        } else {
                            Clock(hourWord(word) ?: return null, 0, null)
                        }
                    index++
                }
            }
        }
        val time =
            clock
                ?: return date?.let {
                    ParsedTime(
                        it.atStartOfDay(now.zone).withHour(MORNING_FROM + 2),
                        dateGiven = true,
                    )
                }
        return resolve(time, hint, date, now)
    }

    private fun hourWord(word: String?): Int? {
        word ?: return null
        word.toIntOrNull()?.let {
            return it
        }
        return Numbers.parse(word)?.toInt()
    }

    private fun resolve(
        clock: Clock,
        hint: Meridiem?,
        date: LocalDate?,
        now: ZonedDateTime,
    ): ParsedTime? {
        if (clock.minute !in 0 until MINUTES_IN_HOUR) return null
        val meridiem = clock.meridiem ?: hint
        val hour =
            when {
                clock.hour > HOURS_IN_HALF_DAY ->
                    if (clock.hour < 24 && meridiem != Meridiem.AM) clock.hour else return null
                meridiem == Meridiem.AM -> if (clock.hour == HOURS_IN_HALF_DAY) 0 else clock.hour
                meridiem == Meridiem.PM ->
                    if (clock.hour == HOURS_IN_HALF_DAY) NOON else clock.hour + HOURS_IN_HALF_DAY
                clock.hour == 0 -> 0
                date != null ->
                    if (clock.hour in MORNING_FROM until HOURS_IN_HALF_DAY) clock.hour
                    else (clock.hour % HOURS_IN_HALF_DAY) + HOURS_IN_HALF_DAY
                else ->
                    return ParsedTime(
                        nextOf(now, clock.hour % HOURS_IN_HALF_DAY, clock.minute),
                        dateGiven = false,
                    )
            }
        val local = LocalTime.of(hour, clock.minute)
        if (date != null) return ParsedTime(date.atTime(local).atZone(now.zone), dateGiven = true)
        var at = now.with(local).truncatedTo(ChronoUnit.MINUTES)
        if (!at.isAfter(now)) at = at.plusDays(1)
        return ParsedTime(at, dateGiven = false)
    }

    private fun nextOf(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
        val morning = now.with(LocalTime.of(hour, minute)).truncatedTo(ChronoUnit.MINUTES)
        val evening = morning.plusHours(HOURS_IN_HALF_DAY.toLong())
        return listOf(morning, evening, morning.plusDays(1)).first { it.isAfter(now) }
    }
}
