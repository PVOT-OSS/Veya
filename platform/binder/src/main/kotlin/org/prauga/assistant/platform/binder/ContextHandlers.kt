// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.intent.IntentHandler

object ContextHandlers {
    fun create(
        context: Context,
        engine: ContextEngine,
        readiness: ContextReadiness,
        vocabulary: VocabularySource,
        grants: IntegrationGrants,
    ): List<IntentHandler> {
        val replies = ContextReplies(context, engine, readiness)
        return listOf(
            ContextSearchHandler(context, replies),
            VocabularyHandler(context, vocabulary, grants, replies),
        )
    }
}
