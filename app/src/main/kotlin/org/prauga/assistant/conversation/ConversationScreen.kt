// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.conversation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.assistant.R
import org.prauga.assistant.core.IntentChoice

@Composable
fun ConversationScreen(viewModel: ConversationViewModel, onShowHistory: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ConversationContent(state, viewModel::onEvent, onShowHistory)
}

@Composable
private fun ConversationContent(
    state: ConversationUiState,
    onEvent: (ConversationEvent) -> Unit,
    onShowHistory: () -> Unit,
) {
    var input by rememberSaveable { mutableStateOf("") }
    val send = {
        if (input.isNotBlank() && !state.busy) {
            onEvent(ConversationEvent.Submit(input))
            input = ""
        }
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        PvotTopBar(
            title = stringResource(R.string.app_name),
            backdrop = null,
            actions = {
                PvotTopBarAction(
                    painterResource(R.drawable.ic_history),
                    stringResource(R.string.action_history),
                    onShowHistory,
                )
                PvotTopBarAction(
                    painterResource(R.drawable.ic_add),
                    stringResource(R.string.action_new_conversation),
                    { onEvent(ConversationEvent.NewConversation) },
                    enabled = state.conversationId != null,
                )
            },
        )
        OnDeviceBadge(Modifier.padding(horizontal = PvotSpacing.L))
        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (state.entries.isEmpty() && state.prompt == null) {
                Welcome(onSuggestion = { onEvent(ConversationEvent.Submit(it)) })
            } else {
                Transcript(state, onEvent)
            }
        }
        InputBar(input, { input = it }, send, canSend = input.isNotBlank() && !state.busy)
    }
}

@Composable
private fun OnDeviceBadge(modifier: Modifier = Modifier) {
    Row(
        modifier.padding(vertical = PvotSpacing.S),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(8.dp),
        ) {}
        Spacer(Modifier.size(PvotSpacing.S))
        Text(
            stringResource(R.string.on_device_badge),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.size(PvotSpacing.S))
        Text(
            stringResource(R.string.on_device_detail),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Welcome(onSuggestion: (String) -> Unit) {
    val suggestions =
        listOf(
            stringResource(R.string.suggestion_battery),
            stringResource(R.string.suggestion_torch),
            stringResource(R.string.suggestion_wifi),
            stringResource(R.string.suggestion_time),
            stringResource(R.string.suggestion_open_camera),
        )
    Column(
        Modifier.fillMaxSize().padding(PvotSpacing.L),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Spacer(Modifier.size(PvotSpacing.S))
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(PvotSpacing.L))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
            suggestions.forEach { suggestion ->
                SuggestionChip(onClick = { onSuggestion(suggestion) }, label = { Text(suggestion) })
            }
        }
    }
}

@Composable
private fun Transcript(state: ConversationUiState, onEvent: (ConversationEvent) -> Unit) {
    val listState = rememberLazyListState()
    val lastKey = state.entries.lastOrNull()?.key
    LaunchedEffect(state.entries.size, state.prompt) {
        val count = state.entries.size + if (state.prompt != null) 1 else 0
        if (count > 0) listState.animateScrollToItem(count - 1)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(PvotSpacing.L),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
    ) {
        items(state.entries, key = { it.key }) { entry ->
            val latency = if (entry.key == lastKey) state.lastReplyLatencyMillis else null
            EntryRow(entry, latency)
        }
        state.prompt?.let { prompt -> item(key = "prompt") { PromptCard(prompt, onEvent) } }
    }
}

@Composable
private fun EntryRow(entry: ConversationEntry, latencyMillis: Long?) {
    when (entry) {
        is ConversationEntry.User -> Bubble(entry.text, fromUser = true)
        is ConversationEntry.Reply -> AssistantBubble(entry.text, latencyMillis)
        is ConversationEntry.Failure -> AssistantBubble(entry.text, null, error = true)
        is ConversationEntry.Unanswered ->
            AssistantBubble(stringResource(R.string.unanswered), null)
    }
}

@Composable
private fun AssistantBubble(text: String, latencyMillis: Long?, error: Boolean = false) {
    Column {
        Bubble(text, fromUser = false, error = error)
        Text(
            if (latencyMillis != null)
                stringResource(R.string.reply_on_device_latency, latencyMillis)
            else stringResource(R.string.reply_on_device),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = PvotSpacing.S, top = PvotSpacing.XS),
        )
    }
}

@Composable
private fun Bubble(text: String, fromUser: Boolean, error: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = if (fromUser) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Surface(
            shape =
                RoundedCornerShape(
                    topStart = BUBBLE_RADIUS,
                    topEnd = BUBBLE_RADIUS,
                    bottomStart = if (fromUser) BUBBLE_RADIUS else BUBBLE_TAIL,
                    bottomEnd = if (fromUser) BUBBLE_TAIL else BUBBLE_RADIUS,
                ),
            color =
                when {
                    fromUser -> colors.primary
                    error -> colors.errorContainer
                    else -> colors.surfaceContainerHigh
                },
            contentColor =
                when {
                    fromUser -> colors.onPrimary
                    error -> colors.onErrorContainer
                    else -> colors.onSurface
                },
            modifier = Modifier.widthIn(max = BUBBLE_MAX_WIDTH),
        ) {
            Text(
                text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.M),
            )
        }
    }
}

@Composable
private fun PromptCard(prompt: ConversationPrompt, onEvent: (ConversationEvent) -> Unit) {
    Surface(
        shape = RoundedCornerShape(BUBBLE_RADIUS),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(PvotSpacing.L),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        ) {
            when (prompt) {
                is ConversationPrompt.Clarify -> {
                    Text(
                        stringResource(R.string.clarify_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
                        prompt.choices.forEach { choice ->
                            SuggestionChip(
                                onClick = { onEvent(ConversationEvent.Choose(choice)) },
                                label = { Text(choice.displayName) },
                            )
                        }
                    }
                }
                is ConversationPrompt.MissingDetail ->
                    Text(
                        stringResource(
                            R.string.missing_detail,
                            prompt.choice.displayName,
                            prompt.parameter,
                        )
                    )
                is ConversationPrompt.Permission ->
                    PermissionPrompt(prompt.choice, prompt.permissions, onEvent)
                ConversationPrompt.PermissionDenied ->
                    Text(stringResource(R.string.permission_denied))
            }
            if (prompt !is ConversationPrompt.Permission) {
                TextButton(onClick = { onEvent(ConversationEvent.DismissPrompt) }) {
                    Text(stringResource(R.string.dismiss))
                }
            }
        }
    }
}

@Composable
private fun PermissionPrompt(
    choice: IntentChoice,
    permissions: Set<String>,
    onEvent: (ConversationEvent) -> Unit,
) {
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            results ->
            onEvent(ConversationEvent.PermissionResult(choice, results.values.all { it }))
        }
    Text(
        stringResource(R.string.permission_title, choice.displayName),
        style = MaterialTheme.typography.titleMedium,
    )
    Text(stringResource(R.string.permission_body), style = MaterialTheme.typography.bodyMedium)
    Row(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
        TextButton(onClick = { launcher.launch(permissions.toTypedArray()) }) {
            Text(stringResource(R.string.permission_allow))
        }
        TextButton(onClick = { onEvent(ConversationEvent.DismissPrompt) }) {
            Text(stringResource(R.string.dismiss))
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    canSend: Boolean,
) {
    Row(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(PvotSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier.weight(1f).onPreviewKeyEvent { event ->
                    val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                    if (enter && !event.isShiftPressed) {
                        if (event.type == KeyEventType.KeyUp) onSend()
                        true
                    } else {
                        false
                    }
                },
            placeholder = { Text(stringResource(R.string.input_hint)) },
            maxLines = 4,
            shape = RoundedCornerShape(INPUT_RADIUS),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
        )
        FilledIconButton(onClick = onSend, enabled = canSend, modifier = Modifier.size(SEND_SIZE)) {
            Icon(
                painterResource(R.drawable.ic_send),
                contentDescription = stringResource(R.string.action_send),
            )
        }
    }
}

private val BUBBLE_RADIUS = 20.dp
private val BUBBLE_TAIL = 6.dp
private val BUBBLE_MAX_WIDTH = 520.dp
private val INPUT_RADIUS = 28.dp
private val SEND_SIZE = 56.dp
