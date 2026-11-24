// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import android.text.format.DateFormat
import java.time.Clock
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
 * Answers questions about days on the phone itself: "how many days until christmas" ([until]) and
 * "what date is 3 weeks from now" (not [until]).
 */
class DateMathHandler(
    private val context: Context,
    lexicon: Lexicon,
    private val until: Boolean,
    private val clock: Clock = Clock.systemDefaultZone(),
) : IntentHandler {
    private val id = if (until) ID_UNTIL else ID_WHICH

    override val descriptor =
        IntentDescriptor(
            id,
            context.getString(if (until) R.string.intent_date_until else R.string.intent_date_which),
        )

    private val matcher = PatternMatcher.from(id, lexicon.phrases(id))
    private val dates = Dates(lexicon)

    override fun match(utterance: Utterance): IntentMatch? {
        val found = matcher.match(utterance) ?: return null
        val subject = found.entities[SUBJECT] ?: return null
        if (dates.parse(subject, ZonedDateTime.now(clock)) == null) return null
        return found.copy(entities = mapOf(SUBJECT to original(utterance.raw, subject)))
    }

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val subject = match.entities[SUBJECT].orEmpty()
        val now = ZonedDateTime.now(clock)
        val date =
            dates.parse(subject, now)
                ?: return IntentOutcome.Failed(context.getString(R.string.date_unclear))
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), date)
        val written = format(date)
        val resources = context.resources
        val text =
            when {
                !until && days < 0 -> context.getString(R.string.date_which_past, written)
                !until -> context.getString(R.string.date_which, written)
                days == 0L -> context.getString(R.string.date_until_today, subject)
                days > 0 ->
                    resources.getQuantityString(
                        R.plurals.date_until,
                        days.toInt(),
                        subject,
                        days.toInt(),
                        written,
                    )
                else ->
                    resources.getQuantityString(
                        R.plurals.date_ago,
                        (-days).toInt(),
                        subject,
                        (-days).toInt(),
                        written,
                    )
            }
        return IntentOutcome.Reply(text)
    }

    private fun format(date: LocalDate): String {
        val locale = context.resources.configuration.locales[0]
        val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMMy")
        return date.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    private fun original(raw: String, fragment: String): String {
        val start = raw.indexOf(fragment, ignoreCase = true)
        return if (start < 0) fragment else raw.substring(start, start + fragment.length)
    }

    companion object {
        const val ID_UNTIL = "date.until"
        const val ID_WHICH = "date.which"
        private const val SUBJECT = "subject"
    }
}
