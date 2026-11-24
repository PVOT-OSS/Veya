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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.sdk.protocol.IContextProvider
import org.prauga.assistant.sdk.protocol.Protocol
import timber.log.Timber

fun interface ContextWarmUp {
    fun warmUp()
}

interface ContextReadiness {
    fun anyEnabled(): Boolean

    suspend fun ensureConnected(timeoutMillis: Long)
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
) : ContextWarmUp, ContextReadiness {
    private class Connection(val binding: ProviderBinding) {
        @Volatile var provider: RemoteContextProvider? = null
    }

    private val connections = ConcurrentHashMap<String, Connection>()
    private val discovered = MutableStateFlow<List<ProviderComponent>>(emptyList())
    private val registrations = MutableStateFlow(0)

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

    override fun anyEnabled(): Boolean = discovered.value.any { grants.isEnabled(it.packageName) }

    override suspend fun ensureConnected(timeoutMillis: Long) {
        warmUp()
        withTimeoutOrNull(timeoutMillis) { registrations.first { allEnabledConnected() } }
    }

    private fun allEnabledConnected(): Boolean =
        discovered.value.all {
            !grants.isEnabled(it.packageName) || connections[it.packageName]?.provider != null
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
                onDisconnected = { connection?.let { lost(packageName, it) } },
                onDied = { reconnect(component) },
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
                val vocabulary =
                    if (remote.protocolVersion() >= Protocol.VERSION_DESCRIBE) {
                        remote.describe().orEmpty().take(Protocol.MAX_TYPES).mapNotNull {
                            it?.toTypeInfo()
                        }
                    } else {
                        remote.types().orEmpty().take(Protocol.MAX_TYPES).mapNotNull {
                            it?.let(::bareTypeInfo)
                        }
                    }
                ContextProviderDescriptor(
                    id = RemoteContextProvider.idFor(component.packageName),
                    packageName = component.packageName,
                    label = component.label,
                    types = vocabulary.mapTo(HashSet()) { it.id },
                    maxSensitivity = remote.maxSensitivity().toSensitivity(),
                    vocabulary = vocabulary,
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
        registrations.update { it + 1 }
    }

    private fun lost(packageName: String, connection: Connection) {
        if (connections[packageName] !== connection) return
        connection.provider = null
        engine.unregister(RemoteContextProvider.idFor(packageName))
    }

    private fun reconnect(component: ProviderComponent) {
        disconnect(component.packageName)
        if (grants.isEnabled(component.packageName)) connect(component)
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
