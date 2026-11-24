// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.ContextQueryParcel
import org.prauga.assistant.sdk.protocol.Protocol

internal fun ContextItem.toParcel(): ContextItemParcel {
    val entries = metadata.entries.take(Protocol.MAX_METADATA_ENTRIES)
    return ContextItemParcel(
        id = id.take(Protocol.MAX_ID_CHARS),
        type = type.take(Protocol.MAX_TYPE_CHARS),
        title = title.take(Protocol.MAX_TITLE_CHARS),
        content = content.take(Protocol.MAX_CONTENT_CHARS),
        timestamp = timestamp,
        expiresAt = expiresAt ?: Protocol.NO_EXPIRY,
        sensitivity = sensitivity.toCode(),
        metadataKeys = Array(entries.size) { entries[it].key.take(Protocol.MAX_METADATA_CHARS) },
        metadataValues = Array(entries.size) { entries[it].value.take(Protocol.MAX_METADATA_CHARS) },
    )
}

internal fun ContextQueryParcel.toQuery(): ContextQuery =
    ContextQuery(
        text = text,
        types = types.toSet(),
        since = since.takeIf { it != ContextQueryParcel.NO_SINCE },
        limit = limit.coerceIn(1, Protocol.MAX_ITEMS_PER_CALL),
    )

internal fun Sensitivity.toCode(): Int =
    when (this) {
        Sensitivity.PUBLIC -> Protocol.SENSITIVITY_PUBLIC
        Sensitivity.PERSONAL -> Protocol.SENSITIVITY_PERSONAL
        Sensitivity.SENSITIVE -> Protocol.SENSITIVITY_SENSITIVE
    }
