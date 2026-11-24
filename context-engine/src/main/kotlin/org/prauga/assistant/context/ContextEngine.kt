// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.context

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

class ContextEngine(
    private val grants: ContextGrants,
    private val clock: () -> Long = System::currentTimeMillis,
    private val providerTimeoutMillis: Long = DEFAULT_PROVIDER_TIMEOUT_MILLIS,
    private val maxTitleChars: Int = DEFAULT_MAX_TITLE_CHARS,
    private val maxContentChars: Int = DEFAULT_MAX_CONTENT_CHARS,
    private val maxItemsPerProvider: Int = DEFAULT_MAX_ITEMS_PER_PROVIDER,
) {
    private val registered = MutableStateFlow<Map<String, ContextProvider>>(emptyMap())
    private val cache = mutableMapOf<CacheKey, CachedItems>()

    val providers: StateFlow<Map<String, ContextProvider>> = registered.asStateFlow()

    fun register(provider: ContextProvider) {
        val id = provider.descriptor.id
        registered.update { it + (id to provider) }
        invalidate(id)
    }

    fun unregister(providerId: String) {
        registered.update { it - providerId }
        invalidate(providerId)
    }

    fun invalidate(providerId: String) {
        synchronized(cache) { cache.keys.removeAll { it.providerId == providerId } }
    }

    suspend fun query(query: ContextQuery): ContextResult = coroutineScope {
        val outcomes =
            registered.value.values.map { provider -> async { ask(provider, query) } }.awaitAll()
        val items =
            outcomes
                .flatMap { it.items }
                .sortedWith(
                    compareByDescending<ContextItem> { relevance(it, query.text) }
                        .thenByDescending { it.timestamp }
                )
                .take(query.limit)
        ContextResult(items, outcomes.mapNotNull { it.skipped })
    }

    private suspend fun ask(provider: ContextProvider, query: ContextQuery): Outcome {
        val descriptor = provider.descriptor
        val grant = grants.grantFor(descriptor)
        if (!grant.enabled) return Outcome.skip(descriptor.id, SkipReason.NOT_GRANTED)
        if (query.types.isNotEmpty() && query.types.none { it in descriptor.types }) {
            return Outcome.skip(descriptor.id, SkipReason.TYPE_NOT_OFFERED)
        }
        val now = clock()
        val key = CacheKey(descriptor.id, query)
        val cached = synchronized(cache) { cache[key]?.takeIf { now - it.storedAt < CACHE_MILLIS } }
        val raw =
            cached?.items
                ?: try {
                    withTimeoutOrNull(providerTimeoutMillis) { provider.query(query) }
                        ?: return Outcome.skip(descriptor.id, SkipReason.TIMED_OUT)
                } catch (e: ContextProviderException) {
                    return Outcome.skip(descriptor.id, SkipReason.FAILED)
                }
        if (cached == null) synchronized(cache) { cache[key] = CachedItems(raw, now) }
        val ceiling = minOf(grant.maxSensitivity, descriptor.maxSensitivity)
        val accepted =
            raw.asSequence()
                .filter { it.source.packageName == descriptor.packageName }
                .filter { it.expiresAt == null || it.expiresAt > now }
                .filter { it.sensitivity <= ceiling }
                .filter { query.types.isEmpty() || it.type in query.types }
                .filter { query.since == null || it.timestamp >= query.since }
                .take(maxItemsPerProvider)
                .map { it.normalized() }
                .toList()
        return Outcome(accepted, null)
    }

    private fun ContextItem.normalized(): ContextItem =
        copy(
            title = title.trim().take(maxTitleChars),
            content = content.trim().take(maxContentChars),
            metadata =
                metadata.entries.take(MAX_METADATA_ENTRIES).associate {
                    it.key.take(MAX_METADATA_CHARS) to it.value.take(MAX_METADATA_CHARS)
                },
        )

    private fun relevance(item: ContextItem, text: String?): Int {
        val words = text?.lowercase()?.split(WHITESPACE)?.filter { it.length > 1 }.orEmpty()
        if (words.isEmpty()) return 0
        val title = item.title.lowercase()
        val content = item.content.lowercase()
        return words.sumOf { word ->
            (if (word in title) TITLE_WEIGHT else 0) + (if (word in content) 1 else 0)
        }
    }

    private data class Outcome(val items: List<ContextItem>, val skipped: SkippedProvider?) {
        companion object {
            fun skip(id: String, reason: SkipReason) =
                Outcome(emptyList(), SkippedProvider(id, reason))
        }
    }

    private data class CacheKey(val providerId: String, val query: ContextQuery)

    private data class CachedItems(val items: List<ContextItem>, val storedAt: Long)

    companion object {
        const val DEFAULT_PROVIDER_TIMEOUT_MILLIS = 300L
        const val DEFAULT_MAX_TITLE_CHARS = 200
        const val DEFAULT_MAX_CONTENT_CHARS = 2_000
        const val DEFAULT_MAX_ITEMS_PER_PROVIDER = 20
        private const val CACHE_MILLIS = 30_000L
        private const val MAX_METADATA_ENTRIES = 16
        private const val MAX_METADATA_CHARS = 200
        private const val TITLE_WEIGHT = 2
        private val WHITESPACE = Regex("\\s+")
    }
}
