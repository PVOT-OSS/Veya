// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import android.os.RemoteException
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.Sensitivity
import org.prauga.assistant.context.SkipReason
import org.prauga.assistant.context.SkippedProvider
import org.prauga.assistant.sdk.protocol.ContextItemParcel
import org.prauga.assistant.sdk.protocol.ContextQueryParcel
import org.prauga.assistant.sdk.protocol.ContextTypeParcel
import org.prauga.assistant.sdk.protocol.IContextProvider
import org.prauga.assistant.sdk.protocol.Protocol
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class LiveProvidersTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val grants = IntegrationGrants(context)
    private val engine = ContextEngine(grants)
    private val vocabulary = VocabularyStore(context)
    private val messages =
        ProviderComponent(
            "org.prauga.messages",
            "org.prauga.assistant.sdk.ContextProviderService",
            "Messages",
        )
    private var installed = listOf(messages)
    private val binder = FakeBinder()
    private val remote = FakeRemote()
    private var now = 0L

    private fun TestScope.live(): LiveProviders {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        return LiveProviders(
                { installed },
                binder,
                engine,
                grants,
                vocabulary,
                backgroundScope,
                dispatcher,
                { now },
                idleMillis = 1_000,
            )
            .also {
                it.start()
                runCurrent()
            }
    }

    private fun enable() =
        grants.set(messages.packageName, ContextGrant(true, Sensitivity.PERSONAL))

    @Test
    fun connectsEnabledAppsAndAnswersFromThem() = runTest {
        enable()
        val live = live()
        live.warmUp()
        binder.connectAll(remote)
        val result = engine.query(ContextQuery(text = "sam"))
        assertEquals(listOf("c1"), result.items.map { it.id })
        val item = result.items.single()
        assertEquals("org.prauga.messages", item.source.packageName)
        assertEquals("Messages", item.source.label)
        val descriptor = engine.providers.value.values.single().descriptor
        assertEquals(setOf("conversation"), descriptor.types)
        val type = descriptor.vocabulary.single()
        assertEquals("Conversations", type.label)
        assertEquals(
            listOf("chats", "x".repeat(Protocol.MAX_WORD_CHARS), "conversation"),
            type.synonyms,
        )
        assertEquals(listOf("new", "unread"), type.flags.single().synonyms)
        assertEquals(listOf(type), vocabulary.apps.value.getValue("org.prauga.messages").types)
        assertEquals("sam", remote.lastQuery?.text)
    }

    @Test
    fun neverBindsAppsTheUserHasNotEnabled() = runTest {
        live().warmUp()
        assertTrue(binder.bound.isEmpty())
    }

    @Test
    fun disablingAnAppDisconnectsIt() = runTest {
        enable()
        val live = live()
        live.warmUp()
        binder.connectAll(remote)
        grants.set(messages.packageName, ContextGrant.Denied)
        runCurrent()
        assertTrue(engine.providers.value.isEmpty())
        assertEquals(1, binder.unbound)
    }

    @Test
    fun aFailingAppIsSkippedNotFatal() = runTest {
        enable()
        val live = live()
        live.warmUp()
        binder.connectAll(remote)
        remote.fail = true
        assertEquals(
            listOf(SkippedProvider("live:org.prauga.messages", SkipReason.FAILED)),
            engine.query(ContextQuery()).skipped,
        )
    }

    @Test
    fun idleConnectionsAreClosed() = runTest {
        enable()
        val live = live()
        live.warmUp()
        binder.connectAll(remote)
        now = 999
        live.sweepIdle()
        assertEquals(0, binder.unbound)
        now = 1_000
        live.sweepIdle()
        assertEquals(1, binder.unbound)
        assertTrue(engine.providers.value.isEmpty())
    }

    @Test
    fun uninstalledAppsAreDropped() = runTest {
        enable()
        val live = live()
        live.warmUp()
        binder.connectAll(remote)
        installed = emptyList()
        live.refreshDirectory()
        runCurrent()
        assertTrue(engine.providers.value.isEmpty())
        assertTrue(live.available.value.isEmpty())
        assertTrue(vocabulary.apps.value.isEmpty())
    }

    @Test
    fun aRestartedAppIsUnregisteredUntilAndroidReconnectsIt() = runTest {
        enable()
        val live = live()
        live.warmUp()
        binder.connectAll(remote)
        binder.disconnectAll()
        assertTrue(engine.providers.value.isEmpty())
        assertEquals(0, binder.unbound)
        binder.connectAll(remote)
        assertEquals(1, engine.providers.value.size)
    }

    @Test
    fun aDeadBindingIsReplaced() = runTest {
        enable()
        val live = live()
        live.warmUp()
        binder.connectAll(remote)
        binder.dieAll()
        assertEquals(1, binder.unbound)
        assertEquals(2, binder.bound.size)
        binder.connectLatest(remote)
        assertEquals(1, engine.providers.value.size)
    }

    @Test
    fun ensureConnectedWaitsForEnabledApps() = runTest {
        enable()
        val live = live()
        assertTrue(live.anyEnabled())
        val waiting = launch { live.ensureConnected(5_000) }
        runCurrent()
        assertTrue(waiting.isActive)
        binder.connectAll(remote)
        runCurrent()
        assertTrue(waiting.isCompleted)
    }

    private class FakeBinder : ProviderBinder {
        val bound = mutableListOf<ProviderComponent>()
        private val callbacks = mutableListOf<Pair<(IContextProvider) -> Unit, () -> Unit>>()
        var unbound = 0

        private val deaths = mutableListOf<() -> Unit>()

        override fun bind(
            component: ProviderComponent,
            onConnected: (IContextProvider) -> Unit,
            onDisconnected: () -> Unit,
            onDied: () -> Unit,
        ): ProviderBinding {
            bound += component
            callbacks += onConnected to onDisconnected
            deaths += onDied
            return ProviderBinding { unbound++ }
        }

        fun connectAll(remote: IContextProvider) = callbacks.toList().forEach { it.first(remote) }

        fun connectLatest(remote: IContextProvider) = callbacks.last().first(remote)

        fun disconnectAll() = callbacks.toList().forEach { it.second() }

        fun dieAll() = deaths.toList().forEach { it() }
    }

    private class FakeRemote : IContextProvider.Stub() {
        var fail = false
        var lastQuery: ContextQueryParcel? = null

        override fun protocolVersion(): Int = Protocol.VERSION

        override fun requestSync() = Unit

        override fun query(query: ContextQueryParcel?): Array<ContextItemParcel> {
            if (fail) throw RemoteException("gone")
            lastQuery = query
            return arrayOf(
                ContextItemParcel(
                    "c1",
                    "conversation",
                    "Sam",
                    "2 unread",
                    10L,
                    Protocol.NO_EXPIRY,
                    Protocol.SENSITIVITY_PERSONAL,
                    emptyArray(),
                    emptyArray(),
                )
            )
        }

        override fun types(): Array<String> = arrayOf("conversation")

        override fun maxSensitivity(): Int = Protocol.SENSITIVITY_PERSONAL

        override fun describe(): Array<ContextTypeParcel> =
            arrayOf(
                ContextTypeParcel(
                    "conversation",
                    "Conversations",
                    arrayOf("Chats", "x".repeat(100)),
                    arrayOf("unread"),
                    arrayOf(arrayOf("New")),
                )
            )
    }
}
