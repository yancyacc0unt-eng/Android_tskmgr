package com.android.tskmgr.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.android.tskmgr.data.PermissionsHelper
import com.android.tskmgr.ui.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            PermissionsHelper.openUsageAccessSettings(context)
                        }
                        .padding(vertical = 8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Usage access", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Allows reading recently used apps to fill the process list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Switch(checked = state.hasUsageAccess, onCheckedChange = null)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Card {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Task Manager for Android 12+.\n\n" +
                        "On modern Android, a non-root app can only see a limited set of running " +
                        "processes. System-wide CPU, memory, storage and network are read from " +
                        "/proc and public APIs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
