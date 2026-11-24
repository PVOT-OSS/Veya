// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.prauga.assistant.context.ContextEngine

class DonatedContextStore
internal constructor(
    private val dao: DonatedItemDao,
    private val engine: ContextEngine,
    private val clock: () -> Long,
    private val maxItemsPerPackage: Int = DEFAULT_MAX_ITEMS_PER_PACKAGE,
) {
    private val mutex = Mutex()
    private val registered = mutableMapOf<String, Registration>()

    suspend fun start() {
        dao.deleteExpired(clock())
        refreshProviders()
    }

    suspend fun donate(
        packageName: String,
        label: String,
        donations: List<Donation>,
    ): DonationResult {
        if (donations.isEmpty()) return DonationResult.Stored(0)
        if (donations.size > MAX_ITEMS_PER_CALL)
            return DonationResult.Rejected(DonationRejection.TOO_MANY_IN_ONE_CALL)
        if (donations.any { !it.isValid() })
            return DonationResult.Rejected(DonationRejection.INVALID_ITEM)
        val ids = donations.map { it.itemId }.distinct()
        val added = ids.size - dao.countExisting(packageName, ids)
        if (dao.count(packageName) + added > maxItemsPerPackage) {
            return DonationResult.Rejected(DonationRejection.QUOTA_EXCEEDED)
        }
        dao.upsert(donations.map { it.toEntity(packageName, label) })
        changed(packageName)
        return DonationResult.Stored(donations.size)
    }

    suspend fun remove(packageName: String, itemIds: List<String>) {
        itemIds.chunked(MAX_ITEMS_PER_CALL).forEach { dao.delete(packageName, it) }
        changed(packageName)
    }

    suspend fun removeAll(packageName: String) {
        dao.deleteAll(packageName)
        changed(packageName)
    }

    fun invalidate(packageName: String) {
        engine.invalidate(DonatedContextProvider.idFor(packageName))
    }

    suspend fun purgeExpired(): Int {
        val removed = dao.deleteExpired(clock())
        if (removed > 0) refreshProviders()
        return removed
    }

    private suspend fun changed(packageName: String) {
        refreshProviders()
        engine.invalidate(DonatedContextProvider.idFor(packageName))
    }

    private suspend fun refreshProviders() {
        val rows = dao.packageTypes()
        mutex.withLock {
            val current =
                rows
                    .groupBy { it.packageName }
                    .mapValues { (_, list) ->
                        Registration(list.first().sourceLabel, list.mapTo(HashSet()) { it.type })
                    }
            (registered.keys - current.keys).forEach {
                engine.unregister(DonatedContextProvider.idFor(it))
            }
            current.forEach { (packageName, registration) ->
                if (registered[packageName] != registration) {
                    engine.register(
                        DonatedContextProvider(
                            packageName,
                            registration.label,
                            registration.types,
                            dao,
                            clock,
                        )
                    )
                }
            }
            registered.clear()
            registered.putAll(current)
        }
    }

    private fun Donation.isValid(): Boolean =
        itemId.isNotBlank() &&
            itemId.length <= MAX_ID_CHARS &&
            type.isNotBlank() &&
            type.length <= MAX_TYPE_CHARS

    private fun Donation.toEntity(packageName: String, label: String) =
        DonatedItemEntity(
            packageName = packageName,
            itemId = itemId,
            type = type,
            title = title.trim().take(ContextEngine.DEFAULT_MAX_TITLE_CHARS),
            content = content.trim().take(ContextEngine.DEFAULT_MAX_CONTENT_CHARS),
            sourceLabel = label,
            timestamp = timestamp,
            expiresAt = expiresAt,
            sensitivity = SensitivityCodes.encode(sensitivity),
            metadata =
                MetadataCodec.encode(
                    metadata.entries.take(MAX_METADATA_ENTRIES).associate {
                        it.key.take(MAX_METADATA_CHARS) to it.value.take(MAX_METADATA_CHARS)
                    }
                ),
        )

    private data class Registration(val label: String, val types: Set<String>)

    companion object {
        const val DEFAULT_MAX_ITEMS_PER_PACKAGE = 5_000
        const val MAX_ITEMS_PER_CALL = 500
        private const val MAX_ID_CHARS = 128
        private const val MAX_TYPE_CHARS = 64
        private const val MAX_METADATA_ENTRIES = 16
        private const val MAX_METADATA_CHARS = 200
    }
}
