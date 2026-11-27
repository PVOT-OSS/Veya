// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import java.text.NumberFormat
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Lexicon
import org.prauga.assistant.understanding.Units

/** Converts units on the phone itself: "5 miles in km", "how many cups in a liter". */
class ConvertHandler(private val context: Context, lexicon: Lexicon) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_convert))

    private val matcher = PatternMatcher.from(ID, lexicon.phrases(ID))
    private val units = Units(lexicon)

    override fun match(utterance: Utterance): IntentMatch? {
        val found = matcher.match(utterance) ?: return null
        val source = found.entities[SOURCE] ?: return null
        val target = found.entities[TARGET] ?: return null
        return found.takeIf { units.convert(source, target) != null }
    }

    override fun closeness(utterance: Utterance): Float = matcher.closeness(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val source = match.entities[SOURCE].orEmpty()
        val target = match.entities[TARGET].orEmpty()
        val value =
            units.convert(source, target)
                ?: return IntentOutcome.Failed(context.getString(R.string.convert_unclear))
        val format = NumberFormat.getInstance(context.resources.configuration.locales[0])
        format.maximumFractionDigits = MAX_FRACTION_DIGITS
        return IntentOutcome.Reply(
            context.getString(R.string.convert_result, source, format.format(value), target)
        )
    }

    companion object {
        const val ID = "math.convert"
        private const val SOURCE = "source"
        private const val TARGET = "target"
        private const val MAX_FRACTION_DIGITS = 6
    }
}
