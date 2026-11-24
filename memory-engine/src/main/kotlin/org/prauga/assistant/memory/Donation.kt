// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import org.prauga.assistant.context.Sensitivity

data class Donation(
    val itemId: String,
    val type: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val sensitivity: Sensitivity,
    val expiresAt: Long? = null,
    val metadata: Map<String, String> = emptyMap(),
)

sealed interface DonationResult {
    data class Stored(val count: Int) : DonationResult

    data class Rejected(val reason: DonationRejection) : DonationResult
}

enum class DonationRejection {
    TOO_MANY_IN_ONE_CALL,
    QUOTA_EXCEEDED,
    INVALID_ITEM,
}
