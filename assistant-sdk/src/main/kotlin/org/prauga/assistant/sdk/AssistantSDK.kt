// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.assistant.sdk

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import java.util.concurrent.atomic.AtomicReference
import org.prauga.assistant.sdk.protocol.IAssistantRegistry
import org.prauga.assistant.sdk.protocol.Protocol

object AssistantSDK {
    private class State(
        val context: Context,
        val config: AssistantConfig,
        val verifier: CallerVerifier,
    )

    private val state = AtomicReference<State?>(null)
    private val provider = AtomicReference<ContextProvider?>(null)

    fun initialize(context: Context, config: AssistantConfig) {
        val app = context.applicationContext
        val signers = PackageSigners(app.packageManager)
        state.set(
            State(
                app,
                config,
                CallerVerifier(app.packageManager::getPackagesForUid, signers::of, config),
            )
        )
    }

    fun registerContextProvider(contextProvider: ContextProvider) {
        provider.set(contextProvider)
    }

    fun unregisterContextProvider() {
        provider.set(null)
    }

    fun notifyChanged() {
        val current = state.get() ?: return
        val intent = Intent(Protocol.ACTION_REGISTRY)
        current.config.assistantPackages.forEach { assistant ->
            val target = Intent(intent).setPackage(assistant)
            val connection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        try {
                            IAssistantRegistry.Stub.asInterface(service)
                                .notifyChanged(current.context.packageName)
                        } catch (e: RemoteException) {
                            current.context.unbindService(this)
                            return
                        }
                        current.context.unbindService(this)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) = Unit
                }
            if (!current.context.bindService(target, connection, Context.BIND_AUTO_CREATE)) {
                current.context.unbindService(connection)
            }
        }
    }

    internal fun verifier(): CallerVerifier? = state.get()?.verifier

    internal fun provider(): ContextProvider? = provider.get()
}
