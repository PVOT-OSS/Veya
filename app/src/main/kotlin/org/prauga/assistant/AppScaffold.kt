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

@Composable
fun AppScaffold() {
    val conversation: ConversationViewModel = hiltViewModel()
    var showHistory by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = showHistory) { showHistory = false }
    Surface(
        Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        if (showHistory) {
            HistoryScreen(
                onBack = { showHistory = false },
                onOpen = { id ->
                    conversation.onEvent(ConversationEvent.Open(id))
                    showHistory = false
                },
            )
        } else {
            ConversationScreen(conversation, onShowHistory = { showHistory = true })
        }
    }
}
