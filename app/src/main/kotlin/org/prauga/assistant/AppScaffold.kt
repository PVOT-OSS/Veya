// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.prauga.assistant.home.HomeScreen

@Composable
fun AppScaffold() {
    Scaffold(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) { padding ->
        Box(
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(top = padding.calculateTopPadding()),
            contentAlignment = Alignment.Center,
        ) {
            HomeScreen()
        }
    }
}
