// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.core.data

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.prauga.assistant.core.model.Item

@Singleton
internal class InMemoryItemRepository @Inject constructor() : ItemRepository {
    private val state = MutableStateFlow<List<Item>>(emptyList())

    override val items: Flow<List<Item>> = state.asStateFlow()

    override suspend fun add(title: String) {
        require(title.isNotBlank()) { "Item title must not be blank" }
        state.update { current ->
            current + Item(id = (current.maxOfOrNull { it.id } ?: 0) + 1, title = title.trim())
        }
    }
}
