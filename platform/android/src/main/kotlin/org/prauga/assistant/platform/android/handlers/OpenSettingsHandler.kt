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
import org.prauga.assistant.intent.PatternMatcher.Companion.rule
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.ActivityLauncher
import org.prauga.assistant.platform.android.R

class OpenSettingsHandler(private val context: Context, private val launcher: ActivityLauncher) :
    IntentHandler {
    override val descriptor = IntentDescriptor(ID, context.getString(R.string.intent_open_settings))

    private val matcher =
        PatternMatcher(
            ID,
            listOf(
                rule("((open|show|go to) )?(the )?(system )?settings", 0.95f),
                rule("((open|show|go to) )?(the )?(?<page>${PAGE_PATTERN}) settings", 0.95f, PAGE),
                rule("(open|show|go to) (the )?(?<page>${PAGE_PATTERN})( page)?", 0.85f, PAGE),
            ),
        )

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

    private enum class Page(
        val action: String,
        @StringRes val label: Int,
        val words: List<String>,
    ) {
        MAIN(Settings.ACTION_SETTINGS, R.string.settings_page_main, emptyList()),
        WIFI(
            Settings.ACTION_WIFI_SETTINGS,
            R.string.settings_page_wifi,
            listOf("wifi", "wi-fi", "wi fi"),
        ),
        BLUETOOTH(
            Settings.ACTION_BLUETOOTH_SETTINGS,
            R.string.settings_page_bluetooth,
            listOf("bluetooth"),
        ),
        DISPLAY(
            Settings.ACTION_DISPLAY_SETTINGS,
            R.string.settings_page_display,
            listOf("display", "screen", "brightness"),
        ),
        SOUND(
            Settings.ACTION_SOUND_SETTINGS,
            R.string.settings_page_sound,
            listOf("sound", "volume"),
        ),
        BATTERY(
            Intent.ACTION_POWER_USAGE_SUMMARY,
            R.string.settings_page_battery,
            listOf("battery", "battery saver"),
        ),
        LOCATION(
            Settings.ACTION_LOCATION_SOURCE_SETTINGS,
            R.string.settings_page_location,
            listOf("location", "gps"),
        ),
        NOTIFICATIONS(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Settings.ACTION_ALL_APPS_NOTIFICATION_SETTINGS
            } else {
                Settings.ACTION_SETTINGS
            },
            R.string.settings_page_notifications,
            listOf("notification", "notifications"),
        ),
        APPS(
            Settings.ACTION_APPLICATION_SETTINGS,
            R.string.settings_page_apps,
            listOf("app", "apps", "application"),
        ),
        STORAGE(
            Settings.ACTION_INTERNAL_STORAGE_SETTINGS,
            R.string.settings_page_storage,
            listOf("storage"),
        ),
        SECURITY(
            Settings.ACTION_SECURITY_SETTINGS,
            R.string.settings_page_security,
            listOf("security", "privacy"),
        ),
        ACCESSIBILITY(
            Settings.ACTION_ACCESSIBILITY_SETTINGS,
            R.string.settings_page_accessibility,
            listOf("accessibility"),
        ),
        DATE(
            Settings.ACTION_DATE_SETTINGS,
            R.string.settings_page_date,
            listOf("date", "time", "date and time"),
        ),
        LANGUAGE(
            Settings.ACTION_LOCALE_SETTINGS,
            R.string.settings_page_language,
            listOf("language", "languages"),
        ),
        NETWORK(
            Settings.ACTION_WIRELESS_SETTINGS,
            R.string.settings_page_network,
            listOf("network", "internet", "mobile data", "data usage"),
        ),
        AIRPLANE(
            Settings.ACTION_AIRPLANE_MODE_SETTINGS,
            R.string.settings_page_airplane,
            listOf("airplane", "airplane mode", "flight mode"),
        ),
    }

    private fun pageFor(word: String): Page? = Page.entries.firstOrNull { word in it.words }

    companion object {
        const val ID = "settings.open"
        private const val PAGE = "page"
        private val PAGE_PATTERN =
            Page.entries
                .flatMap { it.words }
                .sortedByDescending { it.length }
                .joinToString("|") { Regex.escape(it) }
    }
}
