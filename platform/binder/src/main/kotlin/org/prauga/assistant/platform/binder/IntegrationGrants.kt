// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.ContextGrants
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.context.Sensitivity

interface IntegrationAccess {
    fun isEnabled(packageName: String): Boolean
}

class IntegrationGrants(context: Context) : ContextGrants, IntegrationAccess {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val grants = ConcurrentHashMap<String, ContextGrant>()
    private val snapshot = MutableStateFlow<Map<String, ContextGrant>>(emptyMap())

    val all: StateFlow<Map<String, ContextGrant>> = snapshot.asStateFlow()

    init {
        preferences.all.keys
            .filter { it.startsWith(ENABLED_PREFIX) }
            .forEach { key -> load(key.removePrefix(ENABLED_PREFIX)) }
        snapshot.value = HashMap(grants)
    }

    override fun grantFor(provider: ContextProviderDescriptor): ContextGrant =
        grant(provider.packageName)

    override fun isEnabled(packageName: String): Boolean = grant(packageName).enabled

    fun grant(packageName: String): ContextGrant = grants[packageName] ?: ContextGrant.Denied

    fun set(packageName: String, grant: ContextGrant) {
        grants[packageName] = grant
        snapshot.value = HashMap(grants)
        preferences.edit {
            putBoolean(ENABLED_PREFIX + packageName, grant.enabled)
            putInt(CEILING_PREFIX + packageName, encode(grant.maxSensitivity))
        }
    }

    private fun load(packageName: String) {
        grants[packageName] =
            ContextGrant(
                enabled = preferences.getBoolean(ENABLED_PREFIX + packageName, false),
                maxSensitivity = decode(preferences.getInt(CEILING_PREFIX + packageName, PUBLIC)),
            )
    }

    private companion object {
        const val FILE = "integrations"
        const val ENABLED_PREFIX = "enabled."
        const val CEILING_PREFIX = "ceiling."
        const val PUBLIC = 0
        const val PERSONAL = 1
        const val SENSITIVE = 2

        fun encode(sensitivity: Sensitivity): Int =
            when (sensitivity) {
                Sensitivity.PUBLIC -> PUBLIC
                Sensitivity.PERSONAL -> PERSONAL
                Sensitivity.SENSITIVE -> SENSITIVE
            }

        fun decode(code: Int): Sensitivity =
            when (code) {
                PERSONAL -> Sensitivity.PERSONAL
                SENSITIVE -> Sensitivity.SENSITIVE
                else -> Sensitivity.PUBLIC
            }
    }
}
