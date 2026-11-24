// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import androidx.annotation.StringRes
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.ContextResult
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.IntentParameter
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.PatternMatcher.Companion.rule
import org.prauga.assistant.intent.Utterance

class ContextSearchHandler
private constructor(
    private val context: Context,
    private val engine: ContextEngine,
    private val readiness: ContextReadiness,
    private val kind: Kind,
) : IntentHandler {
    enum class Kind(val id: String, @StringRes val label: Int) {
        SEARCH("context.search", R.string.intent_search_context),
        UNREAD("context.unread", R.string.intent_unread_messages),
        FROM("context.from", R.string.intent_messages_from),
    }

    override val descriptor =
        IntentDescriptor(
            kind.id,
            context.getString(kind.label),
            parameters =
                when (kind) {
                    Kind.SEARCH -> listOf(IntentParameter(QUERY, required = true))
                    Kind.FROM -> listOf(IntentParameter(WHO, required = true))
                    Kind.UNREAD -> emptyList()
                },
        )

    private val matcher =
        PatternMatcher(
            kind.id,
            when (kind) {
                Kind.SEARCH ->
                    listOf(
                        rule(
                            "(search|look in|find in) (my )?(?<app>[\\p{L}\\p{N} ]+?) for (?<query>.+)",
                            0.92f,
                            APP,
                            QUERY,
                        ),
                        rule(
                            "(search|find|look up)( for)? (?<query>.+?)( in my apps)?",
                            0.75f,
                            QUERY,
                        ),
                    )
                Kind.UNREAD ->
                    listOf(
                        rule(
                            "((do i have|are there|any|show( me)?|what are)( any| my)? )?unread (messages|texts|sms)( from (?<who>.+))?",
                            0.95f,
                            WHO,
                        )
                    )
                Kind.FROM ->
                    listOf(
                        rule(
                            "((show|any|find)( me)? )?(messages|texts) (from|with) (?<who>.+)",
                            0.9f,
                            WHO,
                        ),
                        rule("what did (?<who>.+?) (say|send|text)( me)?", 0.9f, WHO),
                    )
            },
        )

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val query =
            when (kind) {
                Kind.SEARCH -> ContextQuery(text = match.entities[QUERY], limit = LIMIT)
                Kind.UNREAD ->
                    ContextQuery(
                        text = match.entities[WHO],
                        types = setOf(CONVERSATION),
                        limit = LIMIT,
                    )
                Kind.FROM ->
                    ContextQuery(
                        text = match.entities[WHO],
                        types = setOf(CONVERSATION, MESSAGE),
                        limit = LIMIT,
                    )
            }
        if (!readiness.anyEnabled() && engine.providers.value.isEmpty()) {
            return IntentOutcome.Reply(context.getString(R.string.context_nothing_enabled))
        }
        readiness.ensureConnected(CONNECT_TIMEOUT_MILLIS)
        val result = engine.query(query)
        if (unreachable(result))
            return IntentOutcome.Reply(context.getString(R.string.context_unreachable))
        val app = match.entities[APP]
        val items =
            result.items
                .filter { app == null || it.source.label.contains(app, ignoreCase = true) }
                .filter { kind != Kind.UNREAD || it.metadata[UNREAD] == true.toString() }
        if (items.isEmpty()) {
            val empty =
                if (kind == Kind.UNREAD) context.getString(R.string.context_nothing_unread)
                else context.getString(R.string.context_nothing_found, query.text.orEmpty())
            return IntentOutcome.Reply(empty)
        }
        return IntentOutcome.Reply(format(items))
    }

    private fun unreachable(result: ContextResult): Boolean =
        result.items.isEmpty() && engine.providers.value.isEmpty() && readiness.anyEnabled()

    private fun format(items: List<ContextItem>): String = buildString {
        append(context.getString(R.string.context_results_header))
        items.forEach { item ->
            append('\n')
            val title =
                item.title +
                    if (item.metadata[UNREAD] == true.toString())
                        context.getString(R.string.context_unread_suffix)
                    else ""
            append(
                if (item.content.isBlank())
                    context.getString(R.string.context_result_line, title, item.source.label)
                else
                    context.getString(
                        R.string.context_result_line_text,
                        title,
                        item.content,
                        item.source.label,
                    )
            )
        }
    }

    companion object {
        private const val QUERY = "query"
        private const val APP = "app"
        private const val WHO = "who"
        private const val CONVERSATION = "conversation"
        private const val MESSAGE = "message"
        private const val UNREAD = "unread"
        private const val LIMIT = 5
        private const val CONNECT_TIMEOUT_MILLIS = 300L

        fun all(
            context: Context,
            engine: ContextEngine,
            readiness: ContextReadiness,
        ): List<IntentHandler> =
            Kind.entries.map { ContextSearchHandler(context, engine, readiness, it) }
    }
}
