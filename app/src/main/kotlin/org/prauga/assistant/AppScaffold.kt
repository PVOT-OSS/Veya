// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import org.prauga.assistant.conversation.ConversationEvent
import org.prauga.assistant.conversation.ConversationScreen
import org.prauga.assistant.conversation.ConversationViewModel
import org.prauga.assistant.history.HistoryScreen
import org.prauga.assistant.home.HomeScreen
import org.prauga.assistant.integrations.IntegrationsScreen

@Composable
fun AppScaffold() {
    val conversation: ConversationViewModel = hiltViewModel()
    var stack by rememberSaveable { mutableStateOf(listOf(Screen.HOME)) }
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
    val open: (Screen) -> Unit = { stack = stack + it }
    val back: () -> Unit = { if (stack.size > 1) stack = stack.dropLast(1) }
    BackHandler(enabled = stack.size > 1, onBack = back)
    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        when (stack.last()) {
            Screen.HOME ->
                HomeScreen(
                    onAsk = { text ->
                        draft = text
                        open(Screen.ASK)
                    },
                    onOpenConversation = { id ->
                        conversation.onEvent(ConversationEvent.Open(id))
                        open(Screen.ASK)
                    },
                    onShowIntegrations = { open(Screen.INTEGRATIONS) },
                    onShowHistory = { open(Screen.HISTORY) },
                )
            Screen.ASK ->
                ConversationScreen(
                    conversation,
                    draft = draft,
                    onDraftTaken = { draft = null },
                    onBack = back,
                    onShowHistory = { open(Screen.HISTORY) },
                )
            Screen.HISTORY ->
                HistoryScreen(
                    onBack = back,
                    onOpen = { id ->
                        conversation.onEvent(ConversationEvent.Open(id))
                        stack = stack.dropLast(1).filter { it != Screen.ASK } + Screen.ASK
                    },
                )
            Screen.INTEGRATIONS -> IntegrationsScreen(onBack = back)
        }
    }
}

private enum class Screen {
    HOME,
    ASK,
    HISTORY,
    INTEGRATIONS,
}
