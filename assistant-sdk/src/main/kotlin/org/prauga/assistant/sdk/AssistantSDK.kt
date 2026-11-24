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
    private val config = AtomicReference(AssistantConfig())
    private val provider = AtomicReference<ContextProvider?>(null)
    private val verifier = AtomicReference<CallerVerifier?>(null)

    fun registerContextProvider(contextProvider: ContextProvider) {
        provider.set(contextProvider)
    }

    fun unregisterContextProvider() {
        provider.set(null)
    }

    fun configure(assistantConfig: AssistantConfig) {
        config.set(assistantConfig)
        verifier.set(null)
    }

    fun notifyChanged(context: Context) {
        val app = context.applicationContext
        config.get().assistantPackages.forEach { assistant ->
            val target = Intent(Protocol.ACTION_REGISTRY).setPackage(assistant)
            val connection =
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        try {
                            IAssistantRegistry.Stub.asInterface(service)
                                .notifyChanged(app.packageName)
                        } catch (e: RemoteException) {
                            app.unbindService(this)
                            return
                        }
                        app.unbindService(this)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) = Unit
                }
            if (!app.bindService(target, connection, Context.BIND_AUTO_CREATE))
                app.unbindService(connection)
        }
    }

    internal fun verifier(context: Context): CallerVerifier {
        verifier.get()?.let {
            return it
        }
        val packages = context.applicationContext.packageManager
        val current = config.get()
        val created =
            CallerVerifier(
                packagesForUid = packages::getPackagesForUid,
                signers = PackageSigners(packages)::of,
                trustedSha256 = OfficialSigners.SHA256 + current.additionalTrustedSigners,
                assistantPackages = current.assistantPackages,
            )
        verifier.compareAndSet(null, created)
        return verifier.get() ?: created
    }

    internal fun provider(): ContextProvider? = provider.get()
}
