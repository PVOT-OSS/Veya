// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.memory.Donation
import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.Protocol

internal fun ContextItemParcel.toDonation(): Donation =
    Donation(
        itemId = id,
        type = type,
        title = title,
        content = content,
        timestamp = timestamp,
        sensitivity =
            when (sensitivity) {
                Protocol.SENSITIVITY_PUBLIC -> Sensitivity.PUBLIC
                Protocol.SENSITIVITY_PERSONAL -> Sensitivity.PERSONAL
                else -> Sensitivity.SENSITIVE
            },
        expiresAt = expiresAt.takeIf { it != Protocol.NO_EXPIRY },
        metadata =
            buildMap(minOf(metadataKeys.size, Protocol.MAX_METADATA_ENTRIES)) {
                for (i in 0 until minOf(metadataKeys.size, Protocol.MAX_METADATA_ENTRIES)) {
                    put(metadataKeys[i], metadataValues[i])
                }
            },
    )
