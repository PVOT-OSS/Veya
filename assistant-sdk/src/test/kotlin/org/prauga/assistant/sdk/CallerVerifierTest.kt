// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallerVerifierTest {
    private val pinned =
        "5D:3D:EE:9A:B2:14:FC:9D:07:50:DD:D4:15:9F:DC:33:FF:9B:29:54:62:BB:3F:B6:CD:3C:67:38:9C:B3:40:66"
    private val pinnedBytes = CallerVerifier.parseHex(pinned)
    private val otherBytes = ByteArray(32) { 7 }

    private val uids =
        mapOf(
            VEYA_UID to arrayOf("org.prauga.assistant"),
            IMPOSTOR_UID to arrayOf("org.prauga.assistant.dev"),
            EVIL_UID to arrayOf("com.evil.app"),
            ROTATED_UID to arrayOf("org.prauga.assistant.dev"),
        )

    private fun verifier(
        signers: Map<Pair<Int, String>, SignerSet>,
        pins: Set<String> = setOf(pinned),
    ): CallerVerifier {
        val byPackage = signers.entries.associate { it.key.second to it.value }
        return CallerVerifier(
            { uids[it] },
            { byPackage[it] },
            AssistantConfig(trustedSignerSha256 = pins),
        )
    }

    @Test
    fun trustsOnlyTheAssistantSignedWithAPinnedKey() {
        val signers =
            mapOf(
                (VEYA_UID to "org.prauga.assistant") to SignerSet.Lineage(listOf(pinnedBytes)),
                (IMPOSTOR_UID to "org.prauga.assistant.dev") to
                    SignerSet.Lineage(listOf(otherBytes)),
                (EVIL_UID to "com.evil.app") to SignerSet.Lineage(listOf(pinnedBytes)),
            )
        val verifier = verifier(signers)
        assertTrue(verifier.isTrusted(VEYA_UID))
        assertFalse(verifier.isTrusted(IMPOSTOR_UID))
        assertFalse(verifier.isTrusted(EVIL_UID))
        assertFalse(verifier.isTrusted(UNKNOWN_UID))
    }

    @Test
    fun acceptsAKeyRotatedFromAPinnedKey() {
        val signers =
            mapOf(
                (ROTATED_UID to "org.prauga.assistant.dev") to
                    SignerSet.Lineage(listOf(pinnedBytes, otherBytes))
            )
        assertTrue(verifier(signers).isTrusted(ROTATED_UID))
    }

    @Test
    fun requiresEverySignerOfAMultiSignedApkToBePinned() {
        val mixed =
            mapOf(
                (VEYA_UID to "org.prauga.assistant") to
                    SignerSet.Multiple(listOf(pinnedBytes, otherBytes))
            )
        assertFalse(verifier(mixed).isTrusted(VEYA_UID))
        val allPinned =
            mapOf((VEYA_UID to "org.prauga.assistant") to SignerSet.Multiple(listOf(pinnedBytes)))
        assertTrue(verifier(allPinned).isTrusted(VEYA_UID))
    }

    @Test
    fun failsClosedWithoutPinnedKeys() {
        val signers =
            mapOf((VEYA_UID to "org.prauga.assistant") to SignerSet.Lineage(listOf(pinnedBytes)))
        assertFalse(verifier(signers, pins = emptySet()).isTrusted(VEYA_UID))
    }

    @Test
    fun parsesColonSeparatedAndPlainDigests() {
        assertArrayEquals(
            CallerVerifier.parseHex(
                "5d3dee9ab214fc9d0750ddd4159fdc33ff9b295462bb3fb6cd3c67389cb34066"
            ),
            pinnedBytes,
        )
    }

    private companion object {
        const val VEYA_UID = 10_001
        const val IMPOSTOR_UID = 10_002
        const val EVIL_UID = 10_003
        const val UNKNOWN_UID = 10_004
        const val ROTATED_UID = 10_005
    }
}
