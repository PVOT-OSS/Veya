// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

data class AssistantConfig(
    val additionalTrustedSigners: Set<String> = emptySet(),
    val assistantPackages: Set<String> = DEFAULT_PACKAGES,
) {
    companion object {
        val DEFAULT_PACKAGES = setOf("org.prauga.assistant", "org.prauga.assistant.dev")
    }
}
