// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.core.Assistant
import org.prauga.assistant.core.ConversationStore
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.memory.DonatedContextStore
import org.prauga.assistant.memory.MemoryEngine
import org.prauga.assistant.platform.android.AndroidPlatformGate
import org.prauga.assistant.platform.android.DeviceHandlers
import org.prauga.assistant.platform.binder.IntegrationGrants

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class IoDispatcher

@Qualifier @Retention(AnnotationRetention.BINARY) annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AssistantModule {
    @Provides
    @Singleton
    fun memoryEngine(@ApplicationContext context: Context): MemoryEngine =
        MemoryEngine.create(context)

    @Provides
    @Singleton
    fun conversationStore(memory: MemoryEngine): ConversationStore = memory.conversations

    @Provides
    @Singleton
    fun intentEngine(@ApplicationContext context: Context): IntentEngine =
        IntentEngine(DeviceHandlers.create(context), AndroidPlatformGate(context))

    @Provides
    @Singleton
    fun assistant(engine: IntentEngine, store: ConversationStore): Assistant =
        Assistant(engine, store)

    @Provides @IoDispatcher fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(@IoDispatcher io: CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + io)

    @Provides
    @Singleton
    fun contextEngine(grants: IntegrationGrants): ContextEngine = ContextEngine(grants)

    @Provides
    @Singleton
    fun donatedContext(memory: MemoryEngine, engine: ContextEngine): DonatedContextStore =
        memory.donatedContext(engine)
}
