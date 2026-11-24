// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.os.RemoteException
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.sdk.protocol.IContextProvider
import timber.log.Timber

fun interface ContextWarmUp {
    fun warmUp()
}

class LiveProviders(
    private val directory: ProviderDirectory,
    private val binder: ProviderBinder,
    private val engine: ContextEngine,
    private val grants: IntegrationGrants,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher,
    private val clock: () -> Long = System::currentTimeMillis,
    private val idleMillis: Long = DEFAULT_IDLE_MILLIS,
) : ContextWarmUp {
    private class Connection(val binding: ProviderBinding) {
        @Volatile var provider: RemoteContextProvider? = null
    }

    private val connections = ConcurrentHashMap<String, Connection>()
    private val discovered = MutableStateFlow<List<ProviderComponent>>(emptyList())

    val available: StateFlow<List<ProviderComponent>> = discovered.asStateFlow()

    fun start() {
        scope.launch {
            discovered.value = withContext(io) { directory.discover() }
            grants.all.collect { reconcile() }
        }
        scope.launch {
            while (isActive) {
                delay(SWEEP_MILLIS)
                sweepIdle()
            }
        }
    }

    fun refreshDirectory() {
        scope.launch {
            discovered.value = withContext(io) { directory.discover() }
            reconcile()
        }
    }

    override fun warmUp() {
        discovered.value.forEach { component ->
            if (grants.isEnabled(component.packageName)) connect(component)
        }
    }

    fun invalidate(packageName: String) {
        engine.invalidate(RemoteContextProvider.idFor(packageName))
    }

    internal fun sweepIdle() {
        val now = clock()
        connections.forEach { (packageName, connection) ->
            val provider = connection.provider ?: return@forEach
            if (now - provider.lastUsed.get() >= idleMillis) disconnect(packageName)
        }
    }

    private fun reconcile() {
        val present = discovered.value.mapTo(HashSet()) { it.packageName }
        connections.keys.forEach { packageName ->
            if (packageName !in present || !grants.isEnabled(packageName)) disconnect(packageName)
        }
    }

    private fun connect(component: ProviderComponent) {
        val packageName = component.packageName
        if (connections.containsKey(packageName)) return
        var connection: Connection? = null
        val binding =
            binder.bind(
                component,
                onConnected = { remote ->
                    scope.launch(io) { connection?.let { register(component, remote, it) } }
                },
                onDisconnected = { disconnect(packageName) },
            ) ?: return
        connection = Connection(binding)
        connections[packageName] = connection
    }

    private fun register(
        component: ProviderComponent,
        remote: IContextProvider,
        connection: Connection,
    ) {
        val descriptor =
            try {
                ContextProviderDescriptor(
                    id = RemoteContextProvider.idFor(component.packageName),
                    packageName = component.packageName,
                    label = component.label,
                    types = remote.types().orEmpty().filterNotNull().toSet(),
                    maxSensitivity = remote.maxSensitivity().toSensitivity(),
                )
            } catch (e: RemoteException) {
                Timber.d(e, "Provider left while being described")
                disconnect(component.packageName)
                return
            }
        if (connections[component.packageName] !== connection) return
        val provider = RemoteContextProvider(descriptor, remote, io, clock)
        connection.provider = provider
        engine.register(provider)
    }

    private fun disconnect(packageName: String) {
        val connection = connections.remove(packageName) ?: return
        engine.unregister(RemoteContextProvider.idFor(packageName))
        connection.binding.unbind()
    }

    private companion object {
        const val DEFAULT_IDLE_MILLIS = 120_000L
        const val SWEEP_MILLIS = 30_000L
    }
}
