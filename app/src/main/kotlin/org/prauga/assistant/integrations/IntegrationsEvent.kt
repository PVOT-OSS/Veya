// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.integrations

import org.prauga.assistant.context.Sensitivity

sealed interface IntegrationsEvent {
    data class SetEnabled(val packageName: String, val enabled: Boolean) : IntegrationsEvent

    data class SetLimit(val packageName: String, val limit: Sensitivity) : IntegrationsEvent
}
