// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextProvider
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.Utterance

internal class VocabularyHandler(
    context: Context,
    private val engine: ContextEngine,
    private val replies: ContextReplies,
) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_app_data))

    private class Index(
        val source: Map<String, ContextProvider>,
        val types: Map<String, Set<String>>,
        val flags: Map<String, Set<String>>,
        val patterns: List<Pattern>,
    )

    private class Pattern(val regex: Regex, val groups: List<String>)

    @Volatile private var index: Index? = null

    override fun match(utterance: Utterance): IntentMatch? {
        val current = currentIndex() ?: return null
        val text = utterance.normalized
        for (pattern in current.patterns) {
            val found = pattern.regex.matchEntire(text) ?: continue
            val entities = HashMap<String, String>(4)
            pattern.groups.forEach { name ->
                found.groups[name]
                    ?.value
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let { entities[name] = it }
            }
            if (TYPE !in entities && FLAG !in entities && SUBJECT !in entities) continue
            return IntentMatch(ID, CONFIDENCE, entities)
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
                ?: current.types[MESSAGE_WORDS.firstOrNull { it in current.types }].orEmpty()
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
        val providers = engine.providers.value
        index?.let {
            if (it.source === providers) return it.takeIf { built -> built.types.isNotEmpty() }
        }
        val built = build(providers)
        index = built
        return built.takeIf { it.types.isNotEmpty() }
    }

    private fun build(providers: Map<String, ContextProvider>): Index {
        val types = HashMap<String, MutableSet<String>>()
        val flags = HashMap<String, MutableSet<String>>()
        providers.values.forEach { provider ->
            provider.descriptor.vocabulary.forEach { type ->
                type.synonyms.forEach { types.getOrPut(it) { HashSet() } += type.id }
                type.flags.forEach { flag ->
                    flag.synonyms.forEach { flags.getOrPut(it) { HashSet() } += flag.key }
                }
            }
        }
        if (types.isEmpty()) return Index(providers, emptyMap(), emptyMap(), emptyList())
        val typeAlternation = alternation(types.keys)
        val flagAlternation = if (flags.isEmpty()) NEVER else alternation(flags.keys)
        val messageTypes = MESSAGE_WORDS.any { it in types }
        val patterns =
            listOfNotNull(
                Pattern(
                    Regex(
                        "(?:(?:$FILLER) )*(?:(?<$FLAG>$flagAlternation) )?(?<$TYPE>$typeAlternation)" +
                            "(?: (?:$LINKS) (?<$SUBJECT>.+?))?(?: in (?:my )?(?<$APP>[\\p{L}\\p{N} ]+?)(?: app)?)?"
                    ),
                    listOf(TYPE, FLAG, SUBJECT, APP),
                ),
                Pattern(
                    Regex(
                        "(?:search|look in|find in) (?:my )?(?<$TYPE>$typeAlternation) for (?<$SUBJECT>.+)"
                    ),
                    listOf(TYPE, SUBJECT),
                ),
                if (messageTypes) {
                    Pattern(
                        Regex("what did (?<$SUBJECT>.+?) (?:say|send|text|write)(?: me)?"),
                        listOf(SUBJECT),
                    )
                } else {
                    null
                },
                if (messageTypes)
                    Pattern(Regex("(?:(?:$FILLER) )*(?<$FLAG>$flagAlternation)"), listOf(FLAG))
                else null,
            )
        return Index(providers, types, flags, patterns)
    }

    private fun alternation(words: Set<String>): String =
        words.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }

    companion object {
        const val ID = "context.vocabulary"
        private const val TYPE = "type"
        private const val FLAG = "flag"
        private const val SUBJECT = "subject"
        private const val APP = "app"
        private const val LIMIT = 5
        private const val CONFIDENCE = 0.95f
        private const val NEVER = "(?!)"
        private const val FILLER =
            "any|show|show me|find|list|get|give me|search|search for|do i have|do i have any|are there|" +
                "are there any|what are|what are my|my|all|all my|the|latest|recent"
        private const val LINKS = "from|by|with|about|for|named|called|mentioning|containing|on|of"
        private val MESSAGE_WORDS = listOf("messages", "message", "texts", "text")
    }
}
