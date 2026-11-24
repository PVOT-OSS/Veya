// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk.protocol

object Protocol {
    const val VERSION = 2
    const val VERSION_DESCRIBE = 2

    const val ACTION_REGISTRY = "org.prauga.assistant.sdk.action.REGISTRY"
    const val ACTION_PROVIDER = "org.prauga.assistant.sdk.action.PROVIDER"
    const val PERMISSION_BIND_PROVIDER = "org.prauga.assistant.permission.BIND_CONTEXT_PROVIDER"

    const val MAX_ITEMS_PER_CALL = 500
    const val MAX_ID_CHARS = 128
    const val MAX_TYPE_CHARS = 64
    const val MAX_TITLE_CHARS = 200
    const val MAX_CONTENT_CHARS = 2_000
    const val MAX_METADATA_ENTRIES = 16
    const val MAX_METADATA_CHARS = 200
    const val MAX_TYPES = 32
    const val MAX_SYNONYMS = 16
    const val MAX_FLAGS = 8
    const val MAX_WORD_CHARS = 40

    const val SENSITIVITY_PUBLIC = 0
    const val SENSITIVITY_PERSONAL = 1
    const val SENSITIVITY_SENSITIVE = 2

    const val RESULT_NOT_ENABLED = -1
    const val RESULT_CALLER_MISMATCH = -2
    const val RESULT_TOO_MANY_ITEMS = -3
    const val RESULT_QUOTA_EXCEEDED = -4
    const val RESULT_INVALID_ITEM = -5
    const val RESULT_RATE_LIMITED = -6

    const val NO_EXPIRY = -1L
}
