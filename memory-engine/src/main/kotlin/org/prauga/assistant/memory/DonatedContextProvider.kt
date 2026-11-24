// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextProvider
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.ContextSource
import org.prauga.assistant.context.Sensitivity

internal class DonatedContextProvider(
    packageName: String,
    label: String,
    types: Set<String>,
    private val dao: DonatedItemDao,
    private val clock: () -> Long,
) : ContextProvider {
    override val descriptor =
        ContextProviderDescriptor(
            idFor(packageName),
            packageName,
            label,
            types,
            Sensitivity.SENSITIVE,
        )

    private val source = ContextSource(packageName, label)

    override suspend fun query(query: ContextQuery): List<ContextItem> {
        val match = FtsQuery.from(query.text)
        val anyType = query.types.isEmpty()
        val types = if (anyType) NO_TYPES else query.types.toList()
        val since = query.since ?: Long.MIN_VALUE
        val now = clock()
        val limit = query.limit * OVERFETCH
        val rows =
            if (match != null) {
                dao.search(descriptor.packageName, match, now, since, anyType, types, limit)
            } else {
                dao.recent(descriptor.packageName, now, since, anyType, types, limit)
            }
        return rows.map { row ->
            ContextItem(
                id = row.itemId,
                type = row.type,
                title = row.title,
                content = row.content,
                source = source,
                timestamp = row.timestamp,
                expiresAt = row.expiresAt,
                sensitivity = SensitivityCodes.decode(row.sensitivity),
                metadata = MetadataCodec.decode(row.metadata),
            )
        }
    }

    companion object {
        private const val OVERFETCH = 2
        private val NO_TYPES = listOf("")

        fun idFor(packageName: String) = "donated:$packageName"
    }
}
