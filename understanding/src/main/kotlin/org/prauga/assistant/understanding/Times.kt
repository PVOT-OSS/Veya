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

/**
 * Reads a moment such as "tomorrow at 5", "next friday 8am", "half past seven" or "in 10 minutes".
 * Parsing is strict: a word it can't place means no time at all.
 */
class Times(
    lexicon: Lexicon,
    private val numbers: Numbers = Numbers(lexicon),
    private val durations: Durations = Durations(lexicon, numbers),
) {
    private val fillers = lexicon.table("time.filler")
    private val relative = lexicon.table("time.relative")
    private val next = lexicon.table("time.next")
    private val days = lexicon.table("time.day")
    private val weekdays = lexicon.table("weekday")
    private val meridiems = lexicon.table("time.meridiem")
    private val parts = lexicon.table("time.part")
    private val fixed = lexicon.table("time.fixed")
    private val fractions = lexicon.table("time.fraction")
    private val past = lexicon.table("time.past")
    private val to = lexicon.table("time.to")
    private val starters =
        listOf(fillers, relative, next, days, weekdays, meridiems, parts, fixed, fractions)
    private val clockPattern =
        Regex(
            "(\\d{1,2})(?::(\\d{2}))?(" +
                meridiems.keys
                    .sortedByDescending { it.length }
                    .joinToString("|") { Regex.escape(it) } +
                ")?"
        )

    private class Clock(val hour: Int, val minute: Int, val afterNoon: Boolean?)

    fun parse(text: String, now: ZonedDateTime): ParsedTime? {
        val tokens = Tokens.of(text)
        if (tokens.isEmpty()) return null
        if (tokens.first() in relative) {
            val duration = durations.parse(durations.expand(tokens.drop(1))) ?: return null
            return ParsedTime(now.plus(duration).truncatedTo(ChronoUnit.SECONDS), dateGiven = false)
        }
        var date: LocalDate? = null
        var clock: Clock? = null
        var hint: Boolean? = null
        var index = 0
        while (index < tokens.size) {
            val word = tokens[index]
            val day = days.at(tokens, index)
            val filler = if (day == null) fillers.at(tokens, index) else null
            when {
                day != null -> {
                    date = now.toLocalDate().plusDays(day.value.toLong())
                    parts.at(tokens, index)?.let { hint = it.value > 0 }
                    index += day.consumed
                }
                filler != null -> index += filler.consumed
                word in next && tokens.getOrNull(index + 1) in weekdays -> {
                    val weekday = DayOfWeek.of(weekdays.getValue(tokens[index + 1]))
                    date = now.toLocalDate().with(TemporalAdjusters.next(weekday))
                    index += 2
                }
                word in weekdays -> {
                    val weekday = DayOfWeek.of(weekdays.getValue(word))
                    date = now.toLocalDate().with(TemporalAdjusters.nextOrSame(weekday))
                    index++
                }
                word in fixed -> {
                    val hour = fixed.getValue(word)
                    clock = Clock(hour, 0, hour >= NOON)
                    index++
                }
                word in parts -> {
                    hint = parts.getValue(word) > 0
                    index++
                }
                word in meridiems -> {
                    hint = meridiems.getValue(word) > 0
                    index++
                }
                word in fractions &&
                    (tokens.getOrNull(index + 1) in past || tokens.getOrNull(index + 1) in to) -> {
                    val hour = hourWord(tokens.getOrNull(index + 2)) ?: return null
                    val offset = fractions.getValue(word)
                    clock =
                        if (tokens[index + 1] in past) Clock(hour, offset, null)
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
                    val match = clockPattern.matchEntire(word)
                    clock =
                        if (match != null) {
                            Clock(
                                match.groupValues[1].toInt(),
                                match.groupValues[2].ifEmpty { "0" }.toInt(),
                                match.groupValues[3]
                                    .takeIf { it.isNotEmpty() }
                                    ?.let { meridiems.getValue(it) > 0 },
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
                    ParsedTime(it.atTime(DATE_ONLY_HOUR, 0).atZone(now.zone), dateGiven = true)
                }
        return resolve(time, hint, date, now)
    }

    private fun hourWord(word: String?): Int? {
        word ?: return null
        word.toIntOrNull()?.let {
            return it
        }
        if (!numbers.begins(word)) return null
        val read = numbers.read(listOf(word), 0) ?: return null
        return read.value.toInt().takeIf { read.consumed == 1 }
    }

    /** True when a time could start with [word], so callers can skip text that can't be one. */
    fun begins(word: String): Boolean {
        val lower = word.lowercase()
        return numbers.begins(lower) || starters.any { it.begins(lower) }
    }

    private fun resolve(
        clock: Clock,
        hint: Boolean?,
        date: LocalDate?,
        now: ZonedDateTime,
    ): ParsedTime? {
        if (clock.minute !in 0 until MINUTES_IN_HOUR) return null
        val afterNoon = clock.afterNoon ?: hint
        val hour =
            when {
                clock.hour > HOURS_IN_HALF_DAY ->
                    if (clock.hour < HOURS_IN_DAY && afterNoon != false) clock.hour else return null
                afterNoon == false -> if (clock.hour == HOURS_IN_HALF_DAY) 0 else clock.hour
                afterNoon == true ->
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

    private fun WordTable.getValue(word: String): Int = requireNotNull(get(word)).toInt()

    private companion object {
        const val NOON = 12
        const val MORNING_FROM = 7
        const val DATE_ONLY_HOUR = 9
        const val MINUTES_IN_HOUR = 60
        const val HOURS_IN_HALF_DAY = 12
        const val HOURS_IN_DAY = 24
    }
}
