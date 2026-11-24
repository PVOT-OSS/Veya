// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import org.prauga.assistant.memory.DonatedContextStore
import org.prauga.assistant.memory.Donation
import org.prauga.assistant.memory.DonationResult

interface DonationSink {
    suspend fun donate(
        packageName: String,
        label: String,
        donations: List<Donation>,
    ): DonationResult

    suspend fun remove(packageName: String, itemIds: List<String>)

    suspend fun removeAll(packageName: String)
}

class StoreDonationSink(private val store: DonatedContextStore) : DonationSink {
    override suspend fun donate(packageName: String, label: String, donations: List<Donation>) =
        store.donate(packageName, label, donations)

    override suspend fun remove(packageName: String, itemIds: List<String>) =
        store.remove(packageName, itemIds)

    override suspend fun removeAll(packageName: String) = store.removeAll(packageName)
}
