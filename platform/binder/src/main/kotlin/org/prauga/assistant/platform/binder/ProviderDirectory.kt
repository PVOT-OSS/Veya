// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import org.prauga.assistant.sdk.protocol.Protocol

data class ProviderComponent(val packageName: String, val className: String, val label: String)

fun interface ProviderDirectory {
    fun discover(): List<ProviderComponent>
}

class PackageManagerDirectory(
    private val packages: PackageManager,
    private val ownPackage: String,
) : ProviderDirectory {
    override fun discover(): List<ProviderComponent> {
        val intent = Intent(Protocol.ACTION_PROVIDER)
        val services: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packages.queryIntentServices(intent, PackageManager.ResolveInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION") packages.queryIntentServices(intent, 0)
            }
        return services
            .mapNotNull { info ->
                val service = info.serviceInfo ?: return@mapNotNull null
                if (service.packageName == ownPackage) return@mapNotNull null
                ProviderComponent(
                    service.packageName,
                    service.name,
                    service.applicationInfo.loadLabel(packages).toString(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
}
