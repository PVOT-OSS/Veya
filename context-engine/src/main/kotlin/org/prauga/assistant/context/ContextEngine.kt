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

    suspend fun query(query: ContextQuery): ContextResult {
        val now = clock()
        val words = queryWords(query.text)
        val top = TopItems(query.limit)
        val skipped = ArrayList<SkippedProvider>()
        val toFetch = ArrayList<Pending>()
        for (provider in registered.value.values) {
            val descriptor = provider.descriptor
            val grant = grants.grantFor(descriptor)
            if (!grant.enabled) {
                skipped += SkippedProvider(descriptor.id, SkipReason.NOT_GRANTED)
                continue
            }
            if (query.types.isNotEmpty() && query.types.none { it in descriptor.types }) {
                skipped += SkippedProvider(descriptor.id, SkipReason.TYPE_NOT_OFFERED)
                continue
            }
            val key = CacheKey(descriptor.id, query)
            val cached =
                synchronized(cache) { cache[key]?.takeIf { now - it.storedAt < CACHE_MILLIS } }
            if (cached != null) {
                collect(cached.items, descriptor, grant, query, words, now, top)
            } else {
                toFetch += Pending(provider, grant, key)
            }
        }
        if (toFetch.isNotEmpty()) {
            val fetched = coroutineScope {
                toFetch.map { async { fetch(it.provider, query) } }.awaitAll()
            }
            for (i in toFetch.indices) {
                val pending = toFetch[i]
                val descriptor = pending.provider.descriptor
                when (val outcome = fetched[i]) {
                    is Fetch.Items -> {
                        synchronized(cache) { cache[pending.key] = CachedItems(outcome.items, now) }
                        collect(outcome.items, descriptor, pending.grant, query, words, now, top)
                    }
                    is Fetch.Skipped -> skipped += SkippedProvider(descriptor.id, outcome.reason)
                }
            }
        }
        return ContextResult(top.drain { it.normalized() }, skipped)
    }

    private suspend fun fetch(provider: ContextProvider, query: ContextQuery): Fetch =
        try {
            withTimeoutOrNull(providerTimeoutMillis) { provider.query(query) }
                ?.let { Fetch.Items(it) } ?: Fetch.Skipped(SkipReason.TIMED_OUT)
        } catch (e: ContextProviderException) {
            Fetch.Skipped(SkipReason.FAILED)
        }

    private fun collect(
        items: List<ContextItem>,
        descriptor: ContextProviderDescriptor,
        grant: ContextGrant,
        query: ContextQuery,
        words: Array<String>,
        now: Long,
        top: TopItems,
    ) {
        val ceiling = minOf(grant.maxSensitivity, descriptor.maxSensitivity)
        val since = query.since
        var accepted = 0
        for (i in items.indices) {
            if (accepted == maxItemsPerProvider) return
            val item = items[i]
            if (item.source.packageName != descriptor.packageName) continue
            val expiresAt = item.expiresAt
            if (expiresAt != null && expiresAt <= now) continue
            if (item.sensitivity > ceiling) continue
            if (query.types.isNotEmpty() && item.type !in query.types) continue
            if (since != null && item.timestamp < since) continue
            accepted++
            top.offer(relevance(item, words), item)
        }
    }

    private fun ContextItem.normalized(): ContextItem {
        val cleanTitle = title.trim().take(maxTitleChars)
        val cleanContent = content.trim().take(maxContentChars)
        val cleanMetadata =
            if (
                metadata.size <= MAX_METADATA_ENTRIES &&
                    metadata.all { (k, v) ->
                        k.length <= MAX_METADATA_CHARS && v.length <= MAX_METADATA_CHARS
                    }
            ) {
                metadata
            } else {
                metadata.entries.take(MAX_METADATA_ENTRIES).associate {
                    it.key.take(MAX_METADATA_CHARS) to it.value.take(MAX_METADATA_CHARS)
                }
            }
        if (cleanTitle === title && cleanContent === content && cleanMetadata === metadata)
            return this
        return copy(title = cleanTitle, content = cleanContent, metadata = cleanMetadata)
    }

    private fun relevance(item: ContextItem, words: Array<String>): Int {
        var score = 0
        for (word in words) {
            if (item.title.contains(word, ignoreCase = true)) score += TITLE_WEIGHT
            if (item.content.contains(word, ignoreCase = true)) score += 1
        }
        return score
    }

    private fun queryWords(text: String?): Array<String> {
        if (text.isNullOrBlank()) return NO_WORDS
        return text.lowercase().split(WHITESPACE).filter { it.length > 1 }.toTypedArray()
    }

    private class Pending(
        val provider: ContextProvider,
        val grant: ContextGrant,
        val key: CacheKey,
    )

    private sealed interface Fetch {
        class Items(val items: List<ContextItem>) : Fetch

        class Skipped(val reason: SkipReason) : Fetch
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
        private val NO_WORDS = emptyArray<String>()
    }
}
