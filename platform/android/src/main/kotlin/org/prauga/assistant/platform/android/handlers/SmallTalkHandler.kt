// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import androidx.annotation.StringRes
import java.time.Clock
import java.time.LocalTime
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Lexicon

/** Greetings, thanks and "who are you": short, friendly answers that point to what Veya can do. */
class SmallTalkHandler
private constructor(
    private val context: Context,
    lexicon: Lexicon,
    private val kind: Kind,
    private val clock: Clock,
) : IntentHandler {
    enum class Kind(val id: String, @StringRes val label: Int) {
        GREETING("talk.greeting", R.string.intent_talk_greeting),
        THANKS("talk.thanks", R.string.intent_talk_thanks),
        IDENTITY("talk.identity", R.string.intent_talk_identity),
        HELP("talk.help", R.string.intent_talk_help),
        WELLBEING("talk.wellbeing", R.string.intent_talk_wellbeing),
        GOODBYE("talk.goodbye", R.string.intent_talk_goodbye),
    }

    override val descriptor = IntentDescriptor(kind.id, context.getString(kind.label))

    private val matcher = PatternMatcher.from(kind.id, lexicon.phrases(kind.id))

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val text =
            when (kind) {
                Kind.GREETING ->
                    context.getString(
                        R.string.talk_greeting,
                        context.getString(partOfDay(LocalTime.now(clock))),
                        context.getString(R.string.talk_try),
                    )
                Kind.THANKS -> context.getString(R.string.talk_thanks)
                Kind.IDENTITY ->
                    context.getString(R.string.talk_identity, context.getString(R.string.talk_try))
                Kind.HELP -> context.getString(R.string.talk_help)
                Kind.WELLBEING -> context.getString(R.string.talk_wellbeing)
                Kind.GOODBYE -> context.getString(R.string.talk_goodbye)
            }
        return IntentOutcome.Reply(text)
    }

    @StringRes
    private fun partOfDay(time: LocalTime): Int =
        when (time.hour) {
            in MORNING_FROM until AFTERNOON_FROM -> R.string.talk_morning
            in AFTERNOON_FROM until EVENING_FROM -> R.string.talk_afternoon
            in EVENING_FROM until NIGHT_FROM -> R.string.talk_evening
            else -> R.string.talk_hello
        }

    companion object {
        private const val MORNING_FROM = 5
        private const val AFTERNOON_FROM = 12
        private const val EVENING_FROM = 17
        private const val NIGHT_FROM = 23

        fun all(context: Context, lexicon: Lexicon, clock: Clock = Clock.systemDefaultZone()) =
            Kind.entries.map { SmallTalkHandler(context, lexicon, it, clock) }
    }
}
