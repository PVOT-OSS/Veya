// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.assistant.R
import org.prauga.assistant.capabilities.Capability
import org.prauga.assistant.core.ConversationSummary

@Composable
fun HomeScreen(
    onAsk: (draft: String?) -> Unit,
    onOpenConversation: (String) -> Unit,
    onShowIntegrations: () -> Unit,
    onShowHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var opened by rememberSaveable { mutableStateOf<Capability?>(null) }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            PvotTopBar(
                title = stringResource(R.string.app_name),
                backdrop = null,
                actions = {
                    PvotTopBarAction(
                        painterResource(R.drawable.ic_apps),
                        stringResource(R.string.action_integrations),
                        onShowIntegrations,
                    )
                    PvotTopBarAction(
                        painterResource(R.drawable.ic_history),
                        stringResource(R.string.action_history),
                        onShowHistory,
                    )
                },
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(TILE_MIN_WIDTH),
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = PvotSpacing.L,
                        end = PvotSpacing.L,
                        bottom = PILL_CLEARANCE,
                    ),
                horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S),
                verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            ) {
                fullWidth { OnDeviceLine() }
                fullWidth { SectionTitle(stringResource(R.string.home_capabilities_title)) }
                when {
                    state.scanning ->
                        fullWidth { Hint(stringResource(R.string.home_capabilities_scanning)) }
                    state.capabilities.isEmpty() ->
                        fullWidth { Hint(stringResource(R.string.home_capabilities_none)) }
                    else ->
                        items(state.capabilities, key = { it.capability.name }) { tile ->
                            CapabilityCard(tile) { opened = tile.capability }
                        }
                }
                fullWidth { SectionTitle(stringResource(R.string.home_connected_title)) }
                fullWidth { ConnectedCard(state.connected, state.supported, onShowIntegrations) }
                if (state.recent.isNotEmpty()) {
                    fullWidth { SectionTitle(stringResource(R.string.home_recent_title)) }
                    items(state.recent, key = { it.id }, span = { GridItemSpan(maxLineSpan) }) {
                        conversation ->
                        RecentRow(conversation) { onOpenConversation(conversation.id) }
                    }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(PILL_CLEARANCE)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                    )
                )
        )
        AskPill({ onAsk(null) }, Modifier.align(Alignment.BottomCenter))
    }
    val sheetTile = state.capabilities.firstOrNull { it.capability == opened }
    PvotBottomSheet(
        visible = sheetTile != null,
        onDismissRequest = { opened = null },
        title = sheetTile?.let { stringResource(it.capability.label) },
    ) {
        val example = sheetTile?.example?.let { stringResource(it) }
        if (example != null) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = PvotSpacing.L),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.home_capability_example, example),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        opened = null
                        onAsk(example)
                    }
                ) {
                    Text(stringResource(R.string.home_capability_try))
                }
            }
            Text(
                stringResource(R.string.home_capability_apps_title),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.S),
            )
        }
        sheetTile?.apps?.forEach { app ->
            val chosen = app == sheetTile.preferred
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = ROW_MIN_HEIGHT)
                    .selectable(
                        selected = chosen,
                        role = Role.RadioButton,
                        onClick = { viewModel.choose(sheetTile.capability, app) },
                    )
                    .padding(horizontal = PvotSpacing.L),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    app.label,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                val note =
                    when {
                        chosen -> R.string.home_capability_sheet_chosen
                        sheetTile.preferred == null && app.label == sheetTile.defaultApp ->
                            R.string.home_capability_sheet_default
                        else -> null
                    }
                note?.let {
                    Text(
                        stringResource(it),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        if (sheetTile != null && sheetTile.apps.size > 1) {
            Text(
                stringResource(R.string.home_capability_sheet_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.S),
            )
        }
        Spacer(Modifier.size(PvotSpacing.L))
    }
}

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
}

@Composable
private fun OnDeviceLine() {
    Row(Modifier.padding(bottom = PvotSpacing.S), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(DOT_SIZE),
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
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = PvotSpacing.M),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun CapabilityCard(tile: CapabilityTile, onClick: () -> Unit) {
    PvotCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Column(
            Modifier.padding(PvotSpacing.M),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.XS),
        ) {
            Text(
                stringResource(tile.capability.verb),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            tile.example?.let {
                Text(
                    stringResource(R.string.home_capability_example, stringResource(it)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                pluralStringResource(
                    R.plurals.home_capability_apps,
                    tile.apps.size,
                    tile.apps.size,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                tile.preferred?.let { stringResource(R.string.home_capability_chosen, it.label) }
                    ?: tile.defaultApp?.let { stringResource(R.string.home_capability_default, it) }
                    ?: stringResource(R.string.home_capability_choose),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ConnectedCard(connected: Int, supported: Int, onClick: () -> Unit) {
    PvotCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            if (supported == 0) stringResource(R.string.home_connected_none)
            else
                pluralStringResource(
                    R.plurals.home_connected_summary,
                    supported,
                    connected,
                    supported,
                ),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(PvotSpacing.L),
        )
    }
}

@Composable
private fun RecentRow(conversation: ConversationSummary, onClick: () -> Unit) {
    PvotCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Text(
            conversation.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(PvotSpacing.L),
        )
    }
}

@Composable
private fun AskPill(onAsk: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onAsk,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier =
            modifier
                .navigationBarsPadding()
                .padding(PvotSpacing.L)
                .fillMaxWidth()
                .heightIn(min = PILL_HEIGHT),
    ) {
        Row(
            Modifier.padding(horizontal = PvotSpacing.L),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(R.drawable.ic_send),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.size(PvotSpacing.M))
            Text(
                stringResource(R.string.ask_pill),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val TILE_MIN_WIDTH = 160.dp
private val PILL_HEIGHT = 56.dp
private val PILL_CLEARANCE = 120.dp
private val ROW_MIN_HEIGHT = 48.dp
private val DOT_SIZE = 8.dp
