// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import org.prauga.assistant.capabilities.Capability
import org.prauga.assistant.capabilities.R
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.IntentParameter
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.handlers.AppMatcher
import org.prauga.assistant.understanding.Lexicon
import org.prauga.assistant.understanding.Phrase

/**
 * A command that hands work to whichever installed app can do it, through a standard Android
 * contract. The phrases come from the lexicon under the command's id; the text they capture in
 * `subject` may end by naming an app ("… in Organic Maps"), which then gets the work.
 */
abstract class CapabilityCommand(
    protected val context: Context,
    private val router: AppRouter,
    lexicon: Lexicon,
    id: String,
    @StringRes name: Int,
    @StringRes question: Int? = null,
    private val capability: Capability? = null,
) : IntentHandler {
    sealed interface Plan

    /** What to start, and how to tell the user once an app took it. */
    class Request(val intent: Intent, val done: (app: String) -> String) : Plan

    /** The subject was understood but can't be done, and why. */
    class Refusal(val text: String) : Plan

    override val descriptor =
        IntentDescriptor(
            id,
            context.getString(name),
            parameters =
                question
                    ?.let {
                        listOf(IntentParameter(SUBJECT, required = true, context.getString(it)))
                    }
                    .orEmpty(),
        )

    private val matcher = PatternMatcher.from(id, lexicon.phrases(id))
    private val appTails: List<Phrase> = lexicon.phrases(APP_TAIL)

    /** The work for [subject], or null when it can't be understood. */
    protected abstract fun request(subject: String?, match: IntentMatch): Plan?

    /** Shown when [request] returns null for every reading of the subject. */
    @get:StringRes protected abstract val unclear: Int

    /** Lets a command refuse a phrase match whose subject it can't use. */
    protected open fun understands(subject: String?): Boolean = true

    override fun match(utterance: Utterance): IntentMatch? {
        val found = matcher.match(utterance) ?: return null
        val subject = found.entities[SUBJECT]?.let { original(utterance.raw, it) }
        if (!understands(subject)) return null
        return if (subject == null) found
        else found.copy(entities = found.entities + (SUBJECT to subject))
    }

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val subject = match.entities[SUBJECT]
        var refusal: Refusal? = null
        for ((text, appName) in readings(subject)) {
            val request =
                when (val plan = request(text, match)) {
                    is Request -> plan
                    is Refusal -> {
                        refusal = refusal ?: plan
                        continue
                    }
                    null -> continue
                }
            val apps = router.appsFor(request.intent)
            val named =
                if (appName == null) null
                else AppMatcher.best(appName, apps, wholeWords = true) ?: continue
            return when (val opened = router.open(request.intent, apps, named, capability)) {
                is AppRouter.Opened.App -> IntentOutcome.Reply(request.done(opened.label))
                AppRouter.Opened.Chooser ->
                    IntentOutcome.Reply(context.getString(R.string.command_choose))
                AppRouter.Opened.Nothing ->
                    IntentOutcome.Failed(
                        context.getString(R.string.command_no_app, descriptor.displayName)
                    )
            }
        }
        return IntentOutcome.Failed(refusal?.text ?: context.getString(unclear))
    }

    /** Ways to read [subject]: first as "<text> in <app>", then as plain text. */
    private fun readings(subject: String?): List<Pair<String?, String?>> {
        if (subject == null) return listOf(null to null)
        val lower = subject.lowercase()
        val split =
            appTails.mapNotNull { phrase ->
                val found = phrase.pattern.matchEntire(lower) ?: return@mapNotNull null
                val app = found.groups[APP]?.range ?: return@mapNotNull null
                val rest =
                    if (REST in phrase.slots) found.groups[REST]?.range?.let(subject::substring)
                    else null
                rest to subject.substring(app)
            }
        return (split + (subject to null)).distinct()
    }

    /** [fragment] as the user typed it, so names keep their capitals. */
    private fun original(raw: String, fragment: String): String {
        val start = raw.indexOf(fragment, ignoreCase = true)
        return if (start < 0) fragment else raw.substring(start, start + fragment.length)
    }

    companion object {
        const val SUBJECT = "subject"
        private const val APP = "app"
        private const val REST = "rest"
        private const val APP_TAIL = "command.app"
    }
}
