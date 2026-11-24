// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.context

data class ContextGrant(val enabled: Boolean, val maxSensitivity: Sensitivity) {
    companion object {
        val Denied = ContextGrant(enabled = false, maxSensitivity = Sensitivity.PUBLIC)
    }
}

fun interface ContextGrants {
    fun grantFor(provider: ContextProviderDescriptor): ContextGrant
}
