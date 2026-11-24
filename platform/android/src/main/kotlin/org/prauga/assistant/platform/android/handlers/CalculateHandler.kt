// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import java.math.BigDecimal
import java.math.MathContext
import java.text.NumberFormat
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Arithmetic
import org.prauga.assistant.understanding.Lexicon

/** Answers arithmetic on the phone itself, exactly: "what's 18% of 240", "(2 + 3) × 4". */
class CalculateHandler(private val context: Context, lexicon: Lexicon) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_calculate))

    private val matcher = PatternMatcher.from(ID, lexicon.phrases(ID))
    private val arithmetic = Arithmetic(lexicon)

    override fun match(utterance: Utterance): IntentMatch? {
        if (!arithmetic.mightBe(utterance.normalized)) return null
        val found = matcher.match(utterance) ?: return null
        val subject = found.entities[EXPRESSION] ?: return null
        if (!arithmetic.mightBe(subject)) return null
        val expression = original(utterance.raw, subject)
        val result = arithmetic.evaluate(expression) ?: return null
        val asked = found.confidence >= ASKED
        val accepted =
            when (result) {
                is Arithmetic.Result.Value -> asked || result.operations > 0
                else -> true
            }
        return found.takeIf { accepted }?.copy(entities = mapOf(EXPRESSION to expression))
    }

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val expression = match.entities[EXPRESSION].orEmpty()
        return when (val result = arithmetic.evaluate(expression)) {
            is Arithmetic.Result.Value ->
                IntentOutcome.Reply(
                    context.getString(R.string.calculate_result, expression, format(result.value))
                )
            Arithmetic.Result.DivideByZero ->
                IntentOutcome.Failed(context.getString(R.string.calculate_divide_by_zero))
            Arithmetic.Result.Undefined,
            null -> IntentOutcome.Failed(context.getString(R.string.calculate_undefined))
        }
    }

    private fun format(value: BigDecimal): String {
        if (value.abs() >= LARGE) return value.round(MathContext(SIGNIFICANT)).toString()
        val format = NumberFormat.getInstance(context.resources.configuration.locales[0])
        format.maximumFractionDigits = MAX_FRACTION_DIGITS
        return format.format(value)
    }

    /**
     * [fragment] as the user typed it, with the brackets, signs and `%` that cleaning the text
     * trimmed from its edges.
     */
    private fun original(raw: String, fragment: String): String {
        val found = raw.indexOf(fragment, ignoreCase = true)
        if (found < 0) return fragment
        var start = found
        while (start > 0 && raw[start - 1] in LEADING) start--
        var end = found + fragment.length
        while (end < raw.length && raw[end] in TRAILING) end++
        return raw.substring(start, end).trim()
    }

    companion object {
        const val ID = "math.calculate"
        private const val EXPRESSION = "subject"
        private const val ASKED = 0.9f
        private const val LEADING = "(-−+. "
        private const val TRAILING = ")% "
        private const val MAX_FRACTION_DIGITS = 10
        private const val SIGNIFICANT = 12
        private val LARGE = BigDecimal("1e15")
    }
}
