// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.conversation

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.prauga.assistant.core.Assistant
import org.prauga.assistant.core.ConversationStore
import org.prauga.assistant.core.ConversationSummary
import org.prauga.assistant.core.StoredMessage
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.PlatformGate
import org.prauga.assistant.intent.Utterance

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val store = FakeStore()
    private var warmUps = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun handler(id: String, pattern: String, permissions: Set<String> = emptySet()) =
        object : IntentHandler {
            override val descriptor = IntentDescriptor(id, id, requiredPermissions = permissions)
            private val matcher = PatternMatcher(id, listOf(PatternMatcher.rule(pattern, 0.9f)))

            override fun match(utterance: Utterance) = matcher.match(utterance)

            override suspend fun execute(match: IntentMatch) = IntentOutcome.Reply("$id done")
        }

    private fun viewModel(granted: Set<String> = emptySet()): ConversationViewModel {
        val gate =
            object : PlatformGate {
                override val sdkInt = 36

                override fun isGranted(permission: String) = permission in granted
            }
        val engine =
            IntentEngine(
                listOf(handler("battery", "battery"), handler("cal", "calendar", setOf("CAL"))),
                gate,
            )
        return ConversationViewModel(
            Assistant(engine, store),
            store,
            dispatcher,
            SavedStateHandle(),
            { warmUps++ },
        )
    }

    @Test
    fun submittedCommandsAppearAsUserAndReplyEntries() =
        runTest(dispatcher) {
            val vm = viewModel()
            backgroundScope.launch { vm.state.collect {} }
            vm.onEvent(ConversationEvent.Submit("battery"))
            val state = vm.state.value
            assertEquals(
                listOf("battery", "battery done"),
                state.entries.map {
                    (it as? ConversationEntry.User)?.text ?: (it as ConversationEntry.Reply).text
                },
            )
            assertEquals(false, state.busy)
            assertTrue(state.lastReplyLatencyMillis != null)
        }

    @Test
    fun openQuestionsAreMarkedUnanswered() =
        runTest(dispatcher) {
            val vm = viewModel()
            backgroundScope.launch { vm.state.collect {} }
            vm.onEvent(ConversationEvent.Submit("write a story about a cat"))
            assertTrue(vm.state.value.entries.last() is ConversationEntry.Unanswered)
        }

    @Test
    fun permissionPromptRunsTheCommandOnlyWhenGranted() =
        runTest(dispatcher) {
            val vm = viewModel()
            backgroundScope.launch { vm.state.collect {} }
            vm.onEvent(ConversationEvent.Submit("calendar"))
            val prompt = vm.state.value.prompt as ConversationPrompt.Permission
            vm.onEvent(ConversationEvent.PermissionResult(prompt.choice, granted = false))
            assertEquals(ConversationPrompt.PermissionDenied, vm.state.value.prompt)
            assertEquals(1, vm.state.value.entries.size)
        }

    @Test
    fun newConversationClearsTheTranscript() =
        runTest(dispatcher) {
            val vm = viewModel()
            backgroundScope.launch { vm.state.collect {} }
            vm.onEvent(ConversationEvent.Submit("battery"))
            vm.onEvent(ConversationEvent.NewConversation)
            assertNull(vm.state.value.conversationId)
            assertTrue(vm.state.value.entries.isEmpty())
        }

    @Test
    fun opensAppConnectionsWhenTheScreenStarts() {
        viewModel()
        assertEquals(1, warmUps)
    }

    @Test
    fun blankInputIsIgnored() =
        runTest(dispatcher) {
            val vm = viewModel()
            backgroundScope.launch { vm.state.collect {} }
            vm.onEvent(ConversationEvent.Submit("   "))
            assertNull(vm.state.value.conversationId)
        }

    private class FakeStore : ConversationStore {
        private val messages = MutableStateFlow<Map<String, List<StoredMessage>>>(emptyMap())
        private var next = 0L

        override suspend fun startConversation(title: String, createdAt: Long): String {
            val id = "c${messages.value.size}"
            messages.update { it + (id to emptyList()) }
            return id
        }

        override suspend fun append(conversationId: String, message: StoredMessage) {
            val stored = message.copy(id = ++next)
            messages.update { it + (conversationId to it[conversationId].orEmpty() + stored) }
        }

        override fun conversations(): Flow<List<ConversationSummary>> =
            messages.map { map -> map.keys.map { ConversationSummary(it, it, 0) } }

        override fun messages(conversationId: String): Flow<List<StoredMessage>> =
            messages.map { it[conversationId].orEmpty() }

        override suspend fun delete(conversationId: String) {
            messages.update { it - conversationId }
        }
    }
}
