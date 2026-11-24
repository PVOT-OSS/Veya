// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.ActivityLauncher
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Lexicon

class OpenSettingsHandler(
    private val context: Context,
    private val launcher: ActivityLauncher,
    lexicon: Lexicon,
) : IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_open_settings))

    private val pageWords: Map<String, Page> =
        Page.entries
            .filter { lexicon.has(PAGE_WORDS + it.name.lowercase()) }
            .flatMap { page ->
                lexicon.words(PAGE_WORDS + page.name.lowercase()).map { it to page }
            }
            .toMap()

    private val matcher =
        PatternMatcher.from(ID, lexicon.phrases(ID, mapOf(PAGES to pageWords.keys)))

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val page = match.entities[PAGE]?.let(::pageFor) ?: Page.MAIN
        val opened = launcher.launch(Intent(page.action))
        return if (opened) {
            IntentOutcome.Reply(
                context.getString(R.string.settings_opening, context.getString(page.label))
            )
        } else {
            IntentOutcome.Failed(context.getString(R.string.settings_unavailable))
        }
    }

    private enum class Page(val action: String, @StringRes val label: Int) {
        MAIN(Settings.ACTION_SETTINGS, R.string.settings_page_main),
        WIFI(Settings.ACTION_WIFI_SETTINGS, R.string.settings_page_wifi),
        BLUETOOTH(Settings.ACTION_BLUETOOTH_SETTINGS, R.string.settings_page_bluetooth),
        DISPLAY(Settings.ACTION_DISPLAY_SETTINGS, R.string.settings_page_display),
        SOUND(Settings.ACTION_SOUND_SETTINGS, R.string.settings_page_sound),
        BATTERY(Intent.ACTION_POWER_USAGE_SUMMARY, R.string.settings_page_battery),
        LOCATION(Settings.ACTION_LOCATION_SOURCE_SETTINGS, R.string.settings_page_location),
        NOTIFICATIONS(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS
            } else {
                Settings.ACTION_SETTINGS
            },
            R.string.settings_page_notifications,
        ),
        APPS(Settings.ACTION_APPLICATION_SETTINGS, R.string.settings_page_apps),
        STORAGE(Settings.ACTION_INTERNAL_STORAGE_SETTINGS, R.string.settings_page_storage),
        SECURITY(Settings.ACTION_SECURITY_SETTINGS, R.string.settings_page_security),
        ACCESSIBILITY(Settings.ACTION_ACCESSIBILITY_SETTINGS, R.string.settings_page_accessibility),
        DATE(Settings.ACTION_DATE_SETTINGS, R.string.settings_page_date),
        LANGUAGE(Settings.ACTION_LOCALE_SETTINGS, R.string.settings_page_language),
        NETWORK(Settings.ACTION_WIRELESS_SETTINGS, R.string.settings_page_network),
        AIRPLANE(Settings.ACTION_AIRPLANE_MODE_SETTINGS, R.string.settings_page_airplane),
    }

    private fun pageFor(word: String): Page? = pageWords[word]

    companion object {
        const val ID = "settings.open"
        private const val PAGE = "page"
        private const val PAGES = "pages"
        private const val PAGE_WORDS = "settings.page."
    }
}
