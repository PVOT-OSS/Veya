// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextSource
import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.memory.Donation
import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.Protocol

internal fun Int.toSensitivity(): Sensitivity =
    when (this) {
        Protocol.SENSITIVITY_PUBLIC -> Sensitivity.PUBLIC
        Protocol.SENSITIVITY_PERSONAL -> Sensitivity.PERSONAL
        else -> Sensitivity.SENSITIVE
    }

internal fun ContextItemParcel.toContextItem(source: ContextSource): ContextItem =
    ContextItem(
        id = id,
        type = type,
        title = title,
        content = content,
        source = source,
        timestamp = timestamp,
        expiresAt = expiresAt.takeIf { it != Protocol.NO_EXPIRY },
        sensitivity = sensitivity.toSensitivity(),
        metadata = metadata(),
    )

private fun ContextItemParcel.metadata(): Map<String, String> {
    val count = minOf(metadataKeys.size, Protocol.MAX_METADATA_ENTRIES)
    return buildMap(count) { for (i in 0 until count) put(metadataKeys[i], metadataValues[i]) }
}

internal fun ContextItemParcel.toDonation(): Donation =
    Donation(
        itemId = id,
        type = type,
        title = title,
        content = content,
        timestamp = timestamp,
        sensitivity = sensitivity.toSensitivity(),
        expiresAt = expiresAt.takeIf { it != Protocol.NO_EXPIRY },
        metadata = metadata(),
    )
