// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.annotation.RequiresApi
import java.security.MessageDigest

internal class PackageSigners(private val packages: PackageManager) {
    fun of(packageName: String): SignerSet? =
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) modern(packageName)
            else legacy(packageName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun modern(packageName: String): SignerSet? {
        val info = packages.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val signing = info.signingInfo ?: return null
        return if (signing.hasMultipleSigners()) {
            SignerSet.Multiple(signing.apkContentsSigners.orEmpty().map(::digest))
        } else {
            SignerSet.Lineage(signing.signingCertificateHistory.orEmpty().map(::digest))
        }
    }

    @Suppress("DEPRECATION")
    private fun legacy(packageName: String): SignerSet? {
        val signatures =
            packages.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures.orEmpty()
        return if (signatures.size == 1) SignerSet.Lineage(listOf(digest(signatures[0])))
        else SignerSet.Multiple(signatures.map(::digest))
    }

    private fun digest(signature: Signature): ByteArray =
        MessageDigest.getInstance(SHA_256).digest(signature.toByteArray())

    private companion object {
        const val SHA_256 = "SHA-256"
    }
}
