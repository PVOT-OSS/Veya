// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.history

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.assistant.R
import org.prauga.assistant.core.ConversationSummary

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        PvotTopBar(title = stringResource(R.string.history_title), onBack = onBack, backdrop = null)
        when (val current = state) {
            UiState.Loading -> PvotLoadingContent()
            is UiState.Error ->
                PvotErrorContent(
                    title = stringResource(R.string.history_error_title),
                    message = stringResource(R.string.history_error_message),
                    onRetry = { viewModel.onEvent(HistoryEvent.Retry) },
                )
            is UiState.Success ->
                if (current.data.isEmpty()) {
                    PvotEmptyContent(stringResource(R.string.history_empty))
                } else {
                    ConversationList(
                        current.data,
                        onOpen,
                        { viewModel.onEvent(HistoryEvent.Delete(it)) },
                    )
                }
        }
    }
}

@Composable
private fun ConversationList(
    conversations: List<ConversationSummary>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = PaddingValues(PvotSpacing.L),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
    ) {
        items(conversations, key = { it.id }) { conversation ->
            PvotCard(Modifier.fillMaxWidth().animateItem(), onClick = { onOpen(conversation.id) }) {
                Row(
                    Modifier.padding(
                        start = PvotSpacing.L,
                        top = PvotSpacing.S,
                        bottom = PvotSpacing.S,
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            conversation.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            DateUtils.getRelativeTimeSpanString(conversation.updatedAt).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onDelete(conversation.id) }) {
                        Icon(
                            painterResource(R.drawable.ic_delete),
                            contentDescription = stringResource(R.string.action_delete_conversation),
                        )
                    }
                }
            }
        }
    }
}
