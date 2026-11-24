// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.IntentParameter
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.ActivityLauncher
import org.prauga.assistant.platform.android.R
import org.prauga.assistant.understanding.Lexicon

class OpenAppHandler(
    private val context: Context,
    private val launcher: ActivityLauncher,
    lexicon: Lexicon,
) : IntentHandler {
    override val descriptor =
        IntentDescriptor(
            ID,
            context.getString(R.string.intent_open_app),
            parameters = listOf(IntentParameter(APP, required = true)),
        )

    private val matcher = PatternMatcher.from(ID, lexicon.phrases(ID))

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val query = match.entities[APP].orEmpty()
        val app = AppMatcher.best(query, launchableApps())
        val intent = app?.let { context.packageManager.getLaunchIntentForPackage(it.packageName) }
        if (app == null || intent == null || !launcher.launch(intent)) {
            return IntentOutcome.Failed(context.getString(R.string.app_not_found, query))
        }
        return IntentOutcome.Reply(context.getString(R.string.app_opening, app.label))
    }

    private fun launchableApps(): List<LaunchableApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val activities =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(0),
                )
            } else {
                @Suppress("DEPRECATION") packageManager.queryIntentActivities(launcherIntent, 0)
            }
        return activities
            .map {
                LaunchableApp(it.loadLabel(packageManager).toString(), it.activityInfo.packageName)
            }
            .filter { it.packageName != context.packageName }
    }

    companion object {
        const val ID = "app.open"
        const val APP = "app"
    }
}
