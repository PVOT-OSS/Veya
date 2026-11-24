// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.integrations

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotSegmentedControl
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.assistant.R
import org.prauga.assistant.context.Sensitivity

@Composable
fun IntegrationsScreen(onBack: () -> Unit, viewModel: IntegrationsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        PvotTopBar(
            title = stringResource(R.string.integrations_title),
            onBack = onBack,
            backdrop = null,
        )
        Text(
            stringResource(R.string.integrations_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = PvotSpacing.L),
        )
        if (state.apps.isEmpty()) {
            PvotEmptyContent(stringResource(R.string.integrations_empty))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                contentPadding = PaddingValues(PvotSpacing.L),
                verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
            ) {
                items(state.apps, key = { it.packageName }) { row ->
                    IntegrationCard(row, viewModel::onEvent)
                }
            }
        }
    }
}

@Composable
private fun IntegrationCard(row: IntegrationRow, onEvent: (IntegrationsEvent) -> Unit) {
    val levels = Sensitivity.entries
    val labels =
        listOf(
            stringResource(R.string.sensitivity_public),
            stringResource(R.string.sensitivity_personal),
            stringResource(R.string.sensitivity_sensitive),
        )
    PvotCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(PvotSpacing.L),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.S),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = row.enabled,
                    onCheckedChange = { onEvent(IntegrationsEvent.SetEnabled(row.packageName, it)) },
                )
            }
            if (row.enabled) {
                Text(
                    stringResource(R.string.integrations_limit),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                PvotSegmentedControl(
                    labels,
                    levels.indexOf(row.limit),
                    { index -> onEvent(IntegrationsEvent.SetLimit(row.packageName, levels[index])) },
                )
            }
        }
    }
}
