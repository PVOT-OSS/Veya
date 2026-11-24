// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.intent.IntentOutcome

internal class ContextReplies(
    private val context: Context,
    private val engine: ContextEngine,
    private val readiness: ContextReadiness,
) {
    suspend fun answer(
        query: ContextQuery,
        emptyText: String,
        keep: (ContextItem) -> Boolean = { true },
        tags: (ContextItem) -> List<String> = { emptyList() },
    ): IntentOutcome {
        if (!readiness.anyEnabled() && engine.providers.value.isEmpty()) {
            return IntentOutcome.Reply(context.getString(R.string.context_nothing_enabled))
        }
        readiness.ensureConnected(CONNECT_TIMEOUT_MILLIS)
        val result = engine.query(query)
        if (result.items.isEmpty() && engine.providers.value.isEmpty() && readiness.anyEnabled()) {
            return IntentOutcome.Reply(context.getString(R.string.context_unreachable))
        }
        val items = result.items.filter(keep)
        if (items.isEmpty()) return IntentOutcome.Reply(emptyText)
        return IntentOutcome.Reply(
            buildString {
                append(context.getString(R.string.context_results_header))
                items.forEach { item ->
                    append('\n')
                    val title =
                        item.title +
                            tags(item).joinToString("") {
                                context.getString(R.string.context_flag_suffix, it)
                            }
                    append(
                        if (item.content.isBlank())
                            context.getString(
                                R.string.context_result_line,
                                title,
                                item.source.label,
                            )
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
        )
    }

    fun nothingFound(text: String): String = context.getString(R.string.context_nothing_found, text)

    fun nothingOf(what: String): String = context.getString(R.string.context_nothing_of, what)

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 300L
    }
}
