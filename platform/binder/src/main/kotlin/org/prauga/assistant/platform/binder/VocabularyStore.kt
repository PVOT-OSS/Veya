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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.prauga.assistant.context.ContextFlagInfo
import org.prauga.assistant.context.ContextTypeInfo

data class AppVocabulary(
    val packageName: String,
    val label: String,
    val types: List<ContextTypeInfo>,
)

interface VocabularySource {
    val apps: StateFlow<Map<String, AppVocabulary>>
}

class VocabularyStore(context: Context) : VocabularySource {
    private val preferences: SharedPreferences =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val entries = ConcurrentHashMap<String, AppVocabulary>()
    private val snapshot = MutableStateFlow<Map<String, AppVocabulary>>(emptyMap())

    override val apps: StateFlow<Map<String, AppVocabulary>> = snapshot.asStateFlow()

    init {
        preferences.all.forEach { (packageName, value) ->
            (value as? String)?.let { decode(packageName, it) }?.let { entries[packageName] = it }
        }
        snapshot.value = HashMap(entries)
    }

    fun put(vocabulary: AppVocabulary) {
        if (entries[vocabulary.packageName] == vocabulary) return
        entries[vocabulary.packageName] = vocabulary
        snapshot.value = HashMap(entries)
        preferences.edit { putString(vocabulary.packageName, encode(vocabulary)) }
    }

    fun retainOnly(packageNames: Set<String>) {
        val gone = entries.keys - packageNames
        if (gone.isEmpty()) return
        gone.forEach { entries.remove(it) }
        snapshot.value = HashMap(entries)
        preferences.edit { gone.forEach { remove(it) } }
    }

    private fun encode(vocabulary: AppVocabulary): String =
        JSONObject()
            .put(LABEL, vocabulary.label)
            .put(
                TYPES,
                JSONArray(
                    vocabulary.types.map { type ->
                        JSONObject()
                            .put(ID, type.id)
                            .put(LABEL, type.label)
                            .put(SYNONYMS, JSONArray(type.synonyms))
                            .put(
                                FLAGS,
                                JSONArray(
                                    type.flags.map { flag ->
                                        JSONObject()
                                            .put(KEY, flag.key)
                                            .put(SYNONYMS, JSONArray(flag.synonyms))
                                    }
                                ),
                            )
                    }
                ),
            )
            .toString()

    private fun decode(packageName: String, text: String): AppVocabulary? =
        try {
            val json = JSONObject(text)
            val types = json.getJSONArray(TYPES)
            AppVocabulary(
                packageName,
                json.getString(LABEL),
                (0 until types.length()).map { i ->
                    val type = types.getJSONObject(i)
                    val flags = type.getJSONArray(FLAGS)
                    ContextTypeInfo(
                        id = type.getString(ID),
                        label = type.getString(LABEL),
                        synonyms = type.getJSONArray(SYNONYMS).strings(),
                        flags =
                            (0 until flags.length()).map { j ->
                                val flag = flags.getJSONObject(j)
                                ContextFlagInfo(
                                    flag.getString(KEY),
                                    flag.getJSONArray(SYNONYMS).strings(),
                                )
                            },
                    )
                },
            )
        } catch (e: JSONException) {
            null
        }

    private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }

    private companion object {
        const val FILE = "vocabulary"
        const val LABEL = "label"
        const val TYPES = "types"
        const val ID = "id"
        const val SYNONYMS = "synonyms"
        const val FLAGS = "flags"
        const val KEY = "key"
    }
}
