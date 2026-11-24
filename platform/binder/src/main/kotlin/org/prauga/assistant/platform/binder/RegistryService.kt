// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.os.RemoteException
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.IAssistantRegistry
import org.prauga.assistant.sdk.protocol.IResultCallback
import org.prauga.assistant.sdk.protocol.Protocol
import timber.log.Timber

@AndroidEntryPoint
class RegistryService : Service() {
    @Inject lateinit var gate: DonationGate

    private val binder =
        object : IAssistantRegistry.Stub() {
            override fun protocolVersion(): Int = Protocol.VERSION

            override fun donate(
                packageName: String?,
                items: Array<ContextItemParcel>?,
                callback: IResultCallback?,
            ) {
                gate.donate(
                    Binder.getCallingUid(),
                    packageName.orEmpty(),
                    items.orEmpty().filterNotNull(),
                    callback.asReply(),
                )
            }

            override fun remove(
                packageName: String?,
                itemIds: Array<String>?,
                callback: IResultCallback?,
            ) {
                gate.remove(
                    Binder.getCallingUid(),
                    packageName.orEmpty(),
                    itemIds.orEmpty().filterNotNull(),
                    callback.asReply(),
                )
            }

            override fun removeAll(packageName: String?, callback: IResultCallback?) {
                gate.removeAll(Binder.getCallingUid(), packageName.orEmpty(), callback.asReply())
            }

            override fun notifyChanged(packageName: String?) {
                gate.notifyChanged(Binder.getCallingUid(), packageName.orEmpty())
            }
        }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun IResultCallback?.asReply(): (Int) -> Unit = { code ->
        try {
            this?.onResult(code)
        } catch (e: RemoteException) {
            Timber.d(e, "Caller left before its result arrived")
        }
    }
}
