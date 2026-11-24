// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** The app the user wants Veya to use for each capability, ahead of the system default. */
class PreferredApps(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(context.getSharedPreferences(FILE, Context.MODE_PRIVATE))

    private val state = MutableStateFlow(read())

    val all: StateFlow<Map<Capability, String>> = state.asStateFlow()

    fun of(capability: Capability): String? = state.value[capability]

    /** Makes [packageName] the app for [capability]; null goes back to the system default. */
    fun set(capability: Capability, packageName: String?) {
        state.update { current ->
            if (packageName == null) current - capability else current + (capability to packageName)
        }
        val editor = preferences.edit()
        if (packageName == null) editor.remove(capability.name)
        else editor.putString(capability.name, packageName)
        editor.apply()
    }

    private fun read(): Map<Capability, String> =
        Capability.entries
            .mapNotNull { capability ->
                preferences.getString(capability.name, null)?.let { capability to it }
            }
            .toMap()

    private companion object {
        const val FILE = "preferred_apps"
    }
}
