// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.prauga.assistant.memory.DonationRejection
import org.prauga.assistant.memory.DonationResult
import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.Protocol

class DonationGate(
    private val packagesForUid: (Int) -> Array<String>?,
    private val labelFor: (String) -> String,
    private val access: IntegrationAccess,
    private val limiter: RateLimiter,
    private val sink: DonationSink,
    private val scope: CoroutineScope,
) {
    fun donate(
        uid: Int,
        packageName: String,
        items: List<ContextItemParcel>,
        reply: (Int) -> Unit,
    ) {
        val refused =
            callerProblem(uid, packageName)
                ?: when {
                    !access.isEnabled(packageName) -> Protocol.RESULT_NOT_ENABLED
                    items.size > Protocol.MAX_ITEMS_PER_CALL -> Protocol.RESULT_TOO_MANY_ITEMS
                    else -> null
                }
        if (refused != null) return reply(refused)
        val donations = items.map { it.toDonation() }
        scope.launch { reply(sink.donate(packageName, labelFor(packageName), donations).toCode()) }
    }

    fun remove(uid: Int, packageName: String, itemIds: List<String>, reply: (Int) -> Unit) {
        callerProblem(uid, packageName)?.let {
            return reply(it)
        }
        scope.launch {
            sink.remove(packageName, itemIds)
            reply(itemIds.size)
        }
    }

    fun removeAll(uid: Int, packageName: String, reply: (Int) -> Unit) {
        callerProblem(uid, packageName)?.let {
            return reply(it)
        }
        scope.launch {
            sink.removeAll(packageName)
            reply(0)
        }
    }

    private fun callerProblem(uid: Int, packageName: String): Int? =
        when {
            packagesForUid(uid)?.contains(packageName) != true -> Protocol.RESULT_CALLER_MISMATCH
            !limiter.tryAcquire(uid) -> Protocol.RESULT_RATE_LIMITED
            else -> null
        }

    private fun DonationResult.toCode(): Int =
        when (this) {
            is DonationResult.Stored -> count
            is DonationResult.Rejected ->
                when (reason) {
                    DonationRejection.TOO_MANY_IN_ONE_CALL -> Protocol.RESULT_TOO_MANY_ITEMS
                    DonationRejection.QUOTA_EXCEEDED -> Protocol.RESULT_QUOTA_EXCEEDED
                    DonationRejection.INVALID_ITEM -> Protocol.RESULT_INVALID_ITEM
                }
        }
}
