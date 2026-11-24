// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.integrations

import org.prauga.assistant.context.Sensitivity

data class IntegrationsUiState(val apps: List<IntegrationRow> = emptyList())

data class IntegrationRow(
    val packageName: String,
    val label: String,
    val enabled: Boolean,
    val limit: Sensitivity,
)
