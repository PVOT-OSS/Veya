// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

internal object MemoryEngineAccess {
    fun dao(memory: MemoryEngine): DonatedItemDao = memory.databaseForTests().donations()
}
