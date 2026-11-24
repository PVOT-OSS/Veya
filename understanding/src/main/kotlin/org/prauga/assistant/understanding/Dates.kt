// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

import java.time.DateTimeException
import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Reads a day: "christmas", "25 december", "dec 25th 2027", "3 weeks from now", "10 days ago", "in
 * a fortnight", "next friday", "tomorrow". A day without a year is its next occurrence.
 */
class Dates(
    lexicon: Lexicon,
    private val numbers: Numbers = Numbers(lexicon),
    private val times: Times = Times(lexicon, numbers),
) {
    private val fillers = lexicon.optional("date.filler")
    private val named = lexicon.optional("date.named")
    private val months = lexicon.optional("month")
    private val suffixes = lexicon.optional("date.ordinal")
    private val days = lexicon.optional("date.unit.days")
    private val monthUnits = lexicon.optional("date.unit.months")
    private val into = lexicon.optional("date.in")
    private val after = lexicon.optional("date.after")
    private val before = lexicon.optional("date.before")

    fun parse(text: String, now: ZonedDateTime): LocalDate? {
        val today = now.toLocalDate()
        val tokens = Tokens.of(text).filterIndexed { index, word -> index > 0 || word !in fillers }
        if (tokens.isEmpty()) return null
        return relative(tokens, today)
            ?: named(tokens, today)
            ?: explicit(tokens.filter { it !in fillers }, today)
            ?: times.parse(text, now)?.takeIf { it.dateGiven }?.at?.toLocalDate()
    }

    private fun relative(tokens: List<String>, today: LocalDate): LocalDate? {
        val leading = into.at(tokens, 0)
        val start = leading?.consumed ?: 0
        val amount = numbers.read(tokens, start)
        val unitAt = start + (amount?.consumed ?: 0)
        val dayUnit = days.at(tokens, unitAt)
        val monthUnit = if (dayUnit == null) monthUnits.at(tokens, unitAt) else null
        val unit = dayUnit ?: monthUnit ?: return null
        val count = (amount?.value ?: 1.0).takeIf { it % 1.0 == 0.0 }?.toLong() ?: return null
        val end = unitAt + unit.consumed
        val sign =
            when {
                leading != null && end == tokens.size -> 1L
                leading != null -> return null
                after.at(tokens, end)?.takeIf { end + it.consumed == tokens.size } != null -> 1L
                before.at(tokens, end)?.takeIf { end + it.consumed == tokens.size } != null -> -1L
                else -> return null
            }
        val steps = sign * count * unit.value.toLong()
        return if (dayUnit != null) today.plusDays(steps) else today.plusMonths(steps)
    }

    private fun named(tokens: List<String>, today: LocalDate): LocalDate? {
        val hit = named.at(tokens, 0)?.takeIf { it.consumed == tokens.size } ?: return null
        val code = hit.value.toInt()
        return next(today, code / MONTH_CODE, code % MONTH_CODE, null)
    }

    private fun explicit(tokens: List<String>, today: LocalDate): LocalDate? {
        if (tokens.size !in 2..3) return null
        val year = tokens.getOrNull(2)?.let { YEAR.matchEntire(it)?.value?.toInt() ?: return null }
        val dayFirst = day(tokens[0])?.let { day -> months[tokens[1]]?.let { day to it.toInt() } }
        val monthFirst =
            months[tokens[0]]?.let { month -> day(tokens[1])?.let { it to month.toInt() } }
        val (day, month) = dayFirst ?: monthFirst ?: return null
        return next(today, month, day, year)
    }

    private fun day(word: String): Int? {
        val match = DAY.matchEntire(word) ?: return null
        val suffix = match.groupValues[2]
        if (suffix.isNotEmpty() && suffix !in suffixes) return null
        return match.groupValues[1].toInt()
    }

    private fun next(today: LocalDate, month: Int, day: Int, year: Int?): LocalDate? =
        try {
            if (year != null) LocalDate.of(year, month, day)
            else {
                val thisYear = LocalDate.of(today.year, month, day)
                if (thisYear.isBefore(today)) LocalDate.of(today.year + 1, month, day) else thisYear
            }
        } catch (e: DateTimeException) {
            null
        }

    private companion object {
        const val MONTH_CODE = 100
        val DAY = Regex("(\\d{1,2})(\\p{L}*)")
        val YEAR = Regex("\\d{4}")
    }
}
