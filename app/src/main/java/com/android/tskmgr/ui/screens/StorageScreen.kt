package com.android.tskmgr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tskmgr.data.SystemMetrics
import com.android.tskmgr.ui.StorageViewModel
import com.android.tskmgr.ui.components.PercentBar
import java.util.Locale

@Composable
fun StorageScreen(viewModel: StorageViewModel) {
    val storage by viewModel.storage.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (storage == null) {
            Text("No storage info available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val s = storage!!
            val pct = s.usedBytes.toDouble() / s.totalBytes * 100.0
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Shared Storage", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    PercentBar(pct, MaterialTheme.colorScheme.tertiary, Modifier.fillMaxWidth().height(12.dp))
                    Spacer(Modifier.height(12.dp))
                    Row {
                        Stat("Used", SystemMetrics.formatBytes(s.usedBytes), Modifier.weight(1f))
                        Stat("Free", SystemMetrics.formatBytes(s.freeBytes), Modifier.weight(1f))
                        Stat("Total", SystemMetrics.formatBytes(s.totalBytes), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        String.format(Locale.US, "%.1f%% used", pct),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
