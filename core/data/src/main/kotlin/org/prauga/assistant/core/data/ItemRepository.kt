// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.core.data

import kotlinx.coroutines.flow.Flow
import org.prauga.assistant.core.model.Item

interface ItemRepository {
    val items: Flow<List<Item>>

    suspend fun add(title: String)
}
