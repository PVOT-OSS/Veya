// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import org.prauga.assistant.sdk.protocol.IContextProvider

fun interface ProviderBinding {
    fun unbind()
}

fun interface ProviderBinder {
    fun bind(
        component: ProviderComponent,
        onConnected: (IContextProvider) -> Unit,
        onDisconnected: () -> Unit,
    ): ProviderBinding?
}

class ContextProviderBinder(private val context: Context) : ProviderBinder {
    override fun bind(
        component: ProviderComponent,
        onConnected: (IContextProvider) -> Unit,
        onDisconnected: () -> Unit,
    ): ProviderBinding? {
        val connection =
            object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                    onConnected(IContextProvider.Stub.asInterface(service))
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    onDisconnected()
                }

                override fun onBindingDied(name: ComponentName?) {
                    onDisconnected()
                }

                override fun onNullBinding(name: ComponentName?) {
                    onDisconnected()
                }
            }
        val intent = Intent().setClassName(component.packageName, component.className)
        if (!context.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            context.unbindService(connection)
            return null
        }
        return ProviderBinding { context.unbindService(connection) }
    }
}
