// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.understanding.Lexicon
import org.prauga.assistant.understanding.Phrase

internal class VocabularyHandler(
    context: Context,
    private val lexicon: Lexicon,
    private val vocabulary: VocabularySource,
    private val grants: IntegrationGrants,
    private val replies: ContextReplies,
) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_app_data))

    private class Index(
        val apps: Map<String, AppVocabulary>,
        val granted: Map<String, ContextGrant>,
        val types: Map<String, Set<String>>,
        val flags: Map<String, Set<String>>,
        val phrases: List<Phrase>,
    )

    private val messageWords = lexicon.words(MESSAGE_WORDS)

    @Volatile private var index: Index? = null

    override fun match(utterance: Utterance): IntentMatch? {
        val current = currentIndex() ?: return null
        val text = utterance.normalized
        for (phrase in current.phrases) {
            if (!phrase.mayMatch(text)) continue
            val found = phrase.pattern.matchEntire(text) ?: continue
            val entities = HashMap<String, String>(4)
            phrase.slots.forEach { name ->
                found.groups[name]
                    ?.value
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { entities[name] = it }
            }
            if (TYPE !in entities && FLAG !in entities && SUBJECT !in entities) continue
            return IntentMatch(ID, phrase.confidence, entities)
        }
        return null
    }

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val current =
            currentIndex()
                ?: return replies.answer(ContextQuery(limit = LIMIT), replies.nothingOf(""))
        val typeWord = match.entities[TYPE]
        val flagWord = match.entities[FLAG]
        val typeIds =
            typeWord?.let { current.types[it] }
                ?: current.types[messageWords.firstOrNull { it in current.types }].orEmpty()
        val flagKeys = flagWord?.let { current.flags[it] }.orEmpty()
        val app = match.entities[APP]
        val subject = match.entities[SUBJECT]
        val asked =
            listOfNotNull(flagWord, typeWord).joinToString(" ").ifEmpty { subject.orEmpty() }
        return replies.answer(
            ContextQuery(text = subject, types = typeIds, limit = LIMIT),
            emptyText =
                if (subject != null) replies.nothingFound(subject) else replies.nothingOf(asked),
            keep = { item ->
                (app == null || item.source.label.contains(app, ignoreCase = true)) &&
                    flagKeys.all { item.metadata[it] == true.toString() }
            },
            tags = { item ->
                flagWord
                    ?.takeIf { flagKeys.any { key -> item.metadata[key] == true.toString() } }
                    ?.let(::listOf)
                    .orEmpty()
            },
        )
    }

    private fun currentIndex(): Index? {
        val apps = vocabulary.apps.value
        val granted = grants.all.value
        index?.let {
            if (it.apps === apps && it.granted === granted)
                return it.takeIf { built -> built.types.isNotEmpty() }
        }
        val built = build(apps, granted)
        index = built
        return built.takeIf { it.types.isNotEmpty() }
    }

    private fun build(apps: Map<String, AppVocabulary>, granted: Map<String, ContextGrant>): Index {
        val types = HashMap<String, MutableSet<String>>()
        val flags = HashMap<String, MutableSet<String>>()
        apps.values.forEach { app ->
            if (granted[app.packageName]?.enabled != true) return@forEach
            app.types.forEach { type ->
                type.synonyms.forEach { types.getOrPut(it) { HashSet() } += type.id }
                type.flags.forEach { flag ->
                    flag.synonyms.forEach { flags.getOrPut(it) { HashSet() } += flag.key }
                }
            }
        }
        if (types.isEmpty()) return Index(apps, granted, emptyMap(), emptyMap(), emptyList())
        val lists = mapOf(TYPES to types.keys, FLAGS to flags.keys)
        val phrases =
            lexicon.phrases(ID, lists) +
                if (messageWords.any { it in types }) lexicon.phrases(MESSAGES, lists)
                else emptyList()
        return Index(apps, granted, types, flags, phrases)
    }

    companion object {
        const val ID = "context.vocabulary"
        private const val TYPE = "type"
        private const val FLAG = "flag"
        private const val SUBJECT = "subject"
        private const val APP = "app"
        private const val LIMIT = 5
        private const val TYPES = "types"
        private const val FLAGS = "flags"
        private const val MESSAGES = "context.vocabulary.messages"
        private const val MESSAGE_WORDS = "vocabulary.message"
    }
}
