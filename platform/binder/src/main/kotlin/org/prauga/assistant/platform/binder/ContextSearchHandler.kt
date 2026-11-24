// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.IntentParameter
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.PatternMatcher.Companion.rule
import org.prauga.assistant.intent.Utterance

internal class ContextSearchHandler(context: Context, private val replies: ContextReplies) :
    IntentHandler {
    override val descriptor =
        IntentDescriptor(
            ID,
            context.getString(R.string.intent_search_context),
            parameters = listOf(IntentParameter(QUERY, required = true)),
        )

    private val matcher =
        PatternMatcher(
            ID,
            listOf(
                rule(
                    "(search|look in|find in) (my )?(?<app>[\\p{L}\\p{N} ]+?) for (?<query>.+)",
                    0.8f,
                    APP,
                    QUERY,
                ),
                rule("(search|find|look up)( for)? (?<query>.+?)( in my apps)?", 0.75f, QUERY),
            ),
        )

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val text = match.entities[QUERY].orEmpty()
        val app = match.entities[APP]
        return replies.answer(
            ContextQuery(text = text, limit = LIMIT),
            emptyText = replies.nothingFound(text),
            keep = { app == null || it.source.label.contains(app, ignoreCase = true) },
        )
    }

    companion object {
        const val ID = "context.search"
        private const val QUERY = "query"
        private const val APP = "app"
        private const val LIMIT = 5
    }
}
