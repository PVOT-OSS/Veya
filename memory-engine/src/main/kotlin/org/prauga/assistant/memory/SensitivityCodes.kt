// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.memory

import org.prauga.assistant.context.Sensitivity

internal object SensitivityCodes {
    private const val PUBLIC = 0
    private const val PERSONAL = 1
    private const val SENSITIVE = 2

    fun encode(sensitivity: Sensitivity): Int =
        when (sensitivity) {
            Sensitivity.PUBLIC -> PUBLIC
            Sensitivity.PERSONAL -> PERSONAL
            Sensitivity.SENSITIVE -> SENSITIVE
        }

    fun decode(code: Int): Sensitivity =
        when (code) {
            PUBLIC -> Sensitivity.PUBLIC
            PERSONAL -> Sensitivity.PERSONAL
            else -> Sensitivity.SENSITIVE
        }
}
