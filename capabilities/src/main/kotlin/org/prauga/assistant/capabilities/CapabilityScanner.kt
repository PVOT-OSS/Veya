// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

class CapabilityScanner(private val packages: PackageManager, private val ownPackage: String) {
    fun scan(): CapabilityMap {
        val found = HashMap<String, MutableSet<Capability>>()
        val infos = HashMap<String, ApplicationInfo>()
        val defaults = HashMap<Capability, String>()
        for (capability in Capability.entries) {
            val probe = capability.probe()
            query(probe).forEach { resolved ->
                val activity = resolved.activityInfo ?: return@forEach
                if (activity.packageName == ownPackage) return@forEach
                found.getOrPut(activity.packageName) { HashSet() } += capability
                infos.getOrPut(activity.packageName) { activity.applicationInfo }
            }
            defaultPackage(probe)
                ?.takeIf { it in found && capability in found.getValue(it) }
                ?.let { defaults[capability] = it }
        }
        val apps =
            found.map { (packageName, capabilities) ->
                val info = infos.getValue(packageName)
                AppProfile(
                    packageName,
                    info.loadLabel(packages).toString(),
                    kindOf(info),
                    capabilities,
                )
            }
        return CapabilityMap(apps.sortedBy { it.label.lowercase() }, defaults)
    }

    private fun query(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packages.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packages.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }

    private fun defaultPackage(intent: Intent): String? {
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
        return packageName.takeUnless { it in CHOOSERS }
    }

    private fun kindOf(info: ApplicationInfo): AppKind =
        when (info.category) {
            ApplicationInfo.CATEGORY_GAME -> AppKind.GAME
            ApplicationInfo.CATEGORY_AUDIO -> AppKind.AUDIO
            ApplicationInfo.CATEGORY_VIDEO -> AppKind.VIDEO
            ApplicationInfo.CATEGORY_IMAGE -> AppKind.IMAGE
            ApplicationInfo.CATEGORY_SOCIAL -> AppKind.SOCIAL
            ApplicationInfo.CATEGORY_NEWS -> AppKind.NEWS
            ApplicationInfo.CATEGORY_MAPS -> AppKind.MAPS
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppKind.PRODUCTIVITY
            ApplicationInfo.CATEGORY_ACCESSIBILITY -> AppKind.ACCESSIBILITY
            else -> AppKind.UNDECLARED
        }

    private companion object {
        val CHOOSERS =
            setOf("android", "com.android.intentresolver", "com.google.android.intentresolver")
    }
}
