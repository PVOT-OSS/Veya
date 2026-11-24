// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.understanding.Lexicon

object ContextHandlers {
    fun create(
        context: Context,
        lexicon: Lexicon,
        engine: ContextEngine,
        readiness: ContextReadiness,
        vocabulary: VocabularySource,
        grants: IntegrationGrants,
    ): List<IntentHandler> {
        val replies = ContextReplies(context, engine, readiness)
        return listOf(
            ContextSearchHandler(context, lexicon, replies),
            VocabularyHandler(context, lexicon, vocabulary, grants, replies),
        )
    }
}
