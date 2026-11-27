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
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.understanding.Lexicon

internal class ContextSearchHandler(
    context: Context,
    lexicon: Lexicon,
    private val replies: ContextReplies,
) : IntentHandler {
    override val descriptor =
        IntentDescriptor(
            ID,
            context.getString(R.string.intent_search_context),
            parameters =
                listOf(
                    IntentParameter(
                        QUERY,
                        required = true,
                        context.getString(R.string.search_question),
                    )
                ),
        )

    private val matcher = PatternMatcher.from(ID, lexicon.phrases(ID))

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override fun closeness(utterance: Utterance): Float = matcher.closeness(utterance)

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
