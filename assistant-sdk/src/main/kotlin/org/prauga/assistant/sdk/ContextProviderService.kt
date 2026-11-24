// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.ContextQueryParcel
import org.prauga.assistant.sdk.protocol.IContextProvider
import org.prauga.assistant.sdk.protocol.Protocol

class ContextProviderService : Service() {
    private val binder =
        object : IContextProvider.Stub() {
            override fun protocolVersion(): Int = Protocol.VERSION

            override fun requestSync() {
                verifiedProvider()
            }

            override fun query(query: ContextQueryParcel?): Array<ContextItemParcel> {
                val provider = verifiedProvider() ?: return NO_ITEMS
                val request = query?.toQuery() ?: return NO_ITEMS
                val ceiling = provider.maxSensitivity
                return provider
                    .query(request)
                    .asSequence()
                    .filter { it.sensitivity <= ceiling }
                    .take(request.limit)
                    .map { it.toParcel() }
                    .toList()
                    .toTypedArray()
            }

            override fun types(): Array<String> =
                verifiedProvider()?.types?.toTypedArray() ?: emptyArray()

            override fun maxSensitivity(): Int =
                verifiedProvider()?.maxSensitivity?.toCode() ?: Protocol.SENSITIVITY_PUBLIC
        }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun verifiedProvider(): ContextProvider? {
        val verifier = AssistantSDK.verifier() ?: return null
        if (!verifier.isTrusted(Binder.getCallingUid())) return null
        return AssistantSDK.provider()
    }

    private companion object {
        val NO_ITEMS = emptyArray<ContextItemParcel>()
    }
}
