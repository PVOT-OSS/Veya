// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import org.prauga.assistant.platform.android.handlers.LaunchableApp

/** Finds which installed apps can take an intent and starts the right one. */
class AppRouter(private val context: Context) {
    sealed interface Opened {
        data class App(val label: String) : Opened

        data object Chooser : Opened

        data object Nothing : Opened
    }

    fun appsFor(intent: Intent): List<LaunchableApp> {
        val packages = context.packageManager
        return query(intent)
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .map {
                LaunchableApp(it.applicationInfo.loadLabel(packages).toString(), it.packageName)
            }
    }

    /** Opens [named] if given, else the only or default app, else the system chooser. */
    fun open(intent: Intent, apps: List<LaunchableApp>, named: LaunchableApp?): Opened {
        if (apps.isEmpty()) return Opened.Nothing
        val target = named ?: apps.singleOrNull() ?: defaultOf(intent, apps)
        return try {
            if (target != null) {
                context.startActivity(
                    Intent(intent)
                        .setPackage(target.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Opened.App(target.label)
            } else {
                context.startActivity(
                    Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                Opened.Chooser
            }
        } catch (e: ActivityNotFoundException) {
            Opened.Nothing
        } catch (e: SecurityException) {
            Opened.Nothing
        }
    }

    private fun defaultOf(intent: Intent, apps: List<LaunchableApp>): LaunchableApp? {
        val packages = context.packageManager
        val resolved =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packages.resolveActivity(
                    intent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
                )
            } else {
                @Suppress("DEPRECATION")
                packages.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            }
        val packageName = resolved?.activityInfo?.packageName ?: return null
        return apps.firstOrNull { it.packageName == packageName && packageName !in CHOOSERS }
    }

    private fun query(intent: Intent): List<ResolveInfo> {
        val packages = context.packageManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packages.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
    }

    companion object {
        internal val CHOOSERS =
            setOf("android", "com.android.intentresolver", "com.google.android.intentresolver")
    }
}
