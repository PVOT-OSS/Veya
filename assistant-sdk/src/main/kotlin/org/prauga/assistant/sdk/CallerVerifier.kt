// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

internal class CallerVerifier(
    private val packagesForUid: (Int) -> Array<String>?,
    private val signers: (String) -> SignerSet?,
    config: AssistantConfig,
) {
    private val allowedPackages = config.assistantPackages
    private val trusted: List<ByteArray> = config.trustedSignerSha256.map(::parseHex)

    fun isTrusted(uid: Int): Boolean {
        if (trusted.isEmpty()) return false
        val names = packagesForUid(uid) ?: return false
        return names.any { it in allowedPackages && signedByTrustedKey(it) }
    }

    private fun signedByTrustedKey(packageName: String): Boolean {
        val set = signers(packageName) ?: return false
        return when (set) {
            is SignerSet.Multiple -> set.digests.isNotEmpty() && set.digests.all(::isPinned)
            is SignerSet.Lineage -> set.digests.any(::isPinned)
        }
    }

    private fun isPinned(digest: ByteArray): Boolean = trusted.any { it.contentEquals(digest) }

    companion object {
        fun parseHex(text: String): ByteArray {
            val clean = text.filter { it.isLetterOrDigit() }
            require(clean.length == SHA256_HEX_CHARS) {
                "A SHA-256 digest has $SHA256_HEX_CHARS hex characters"
            }
            return ByteArray(clean.length / 2) { i ->
                clean.substring(i * 2, i * 2 + 2).toInt(HEX).toByte()
            }
        }

        private const val SHA256_HEX_CHARS = 64
        private const val HEX = 16
    }
}

internal sealed interface SignerSet {
    val digests: List<ByteArray>

    class Multiple(override val digests: List<ByteArray>) : SignerSet

    class Lineage(override val digests: List<ByteArray>) : SignerSet
}
