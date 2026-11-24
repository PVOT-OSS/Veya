// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.intent

class FakeHandler(
    id: String,
    vararg rules: PatternMatcher.Rule,
    parameters: List<IntentParameter> = emptyList(),
    permissions: Set<String> = emptySet(),
    minSdk: Int = 1,
    maxSdk: Int? = null,
) : IntentHandler {
    override val descriptor = IntentDescriptor(id, id, parameters, permissions, minSdk, maxSdk)

    private val matcher = PatternMatcher(id, rules.toList())

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome =
        IntentOutcome.Reply(match.intentId)
}

class FakeGate(override val sdkInt: Int = 36, private val granted: Set<String> = emptySet()) :
    PlatformGate {
    override fun isGranted(permission: String): Boolean = permission in granted
}
