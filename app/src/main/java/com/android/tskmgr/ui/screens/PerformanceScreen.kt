package com.android.tskmgr.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tskmgr.data.SystemMetrics
import com.android.tskmgr.ui.PerformanceUiState
import com.android.tskmgr.ui.PerformanceViewModel
import com.android.tskmgr.ui.components.HistoryChart
import com.android.tskmgr.ui.components.PercentBar
import java.util.Locale

@Composable
fun PerformanceScreen(viewModel: PerformanceViewModel) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        CpuCard(state)
        MemoryCard(state.memory)
        StorageCard(state.storage)
        NetworkCard(state)
    }
}

@Composable
private fun CpuCard(state: PerformanceUiState) {
    val cpuPercent = state.cpuPercent
    val value = when {
        cpuPercent != null -> String.format(Locale.US, "%.1f%%", cpuPercent)
        state.loadAvg != null -> String.format(Locale.US, "Load %.2f", state.loadAvg)
        else -> "Unavailable"
    }
    SurfaceCard(title = "CPU", value = value) {
        when {
            cpuPercent != null -> {
                HistoryChart(values = state.cpuHistory.map { it.toFloat() }, color = MaterialTheme.colorScheme.primary, maxValue = 100f, modifier = Modifier.height(120.dp))
                if (state.cores.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Cores",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.cores.forEach { core ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                PercentBar(core, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(8.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(String.format(Locale.US, "%.0f%%", core), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            state.loadAvg != null -> {
                Text(
                    text = "System CPU % is not readable by non-root apps on Android 12+. Showing the 1-minute load average instead (cores busy = load ≈ core count).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Text(
                    text = "Android 12+ blocks non-root apps from reading /proc/stat, so system CPU usage is not available. Load average is not readable either on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MemoryCard(memory: SystemMetrics.MemoryInfo?) {
    SurfaceCard(title = "Memory", value = memory?.let { String.format(Locale.US, "%.1f%%", it.usedPercent) } ?: "--") {
        if (memory != null) {
            PercentBar(memory.usedPercent, MaterialTheme.colorScheme.secondary, Modifier.fillMaxWidth().height(12.dp))
            Spacer(Modifier.height(12.dp))
            Row {
                Metric("Used", SystemMetrics.formatBytes(memory.usedMb * 1048576L), Modifier.weight(1f))
                Metric("Available", SystemMetrics.formatBytes(memory.freeMb * 1048576L), Modifier.weight(1f))
                Metric("Cached", SystemMetrics.formatBytes(memory.cachedMb * 1048576L), Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Total ${SystemMetrics.formatBytes(memory.totalMb * 1048576L)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StorageCard(storage: SystemMetrics.StorageInfo?) {
    val pct = storage?.let { it.usedBytes.toDouble() / it.totalBytes * 100.0 } ?: 0.0
    SurfaceCard(title = "Storage", value = String.format(Locale.US, "%.1f%%", pct)) {
        if (storage != null) {
            PercentBar(pct, MaterialTheme.colorScheme.tertiary, Modifier.fillMaxWidth().height(12.dp))
            Spacer(Modifier.height(12.dp))
            Row {
                Metric("Used", SystemMetrics.formatBytes(storage.usedBytes), Modifier.weight(1f))
                Metric("Free", SystemMetrics.formatBytes(storage.freeBytes), Modifier.weight(1f))
                Metric("Total", SystemMetrics.formatBytes(storage.totalBytes), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun NetworkCard(state: PerformanceUiState) {
    val rxMax = state.rxHistory.maxOrNull()?.toFloat() ?: 1f
    val txMax = state.txHistory.maxOrNull()?.toFloat() ?: 1f
    SurfaceCard(title = "Network", value = "RX ${SystemMetrics.formatBytes(state.rxHistory.lastOrNull() ?: 0L)}/s") {
        if (!state.networkAvailable) {
            Text(
                text = "Network counters are not readable. Grant \"Usage access\" in Settings to enable network monitoring.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }
        Row {
            Column(Modifier.weight(1f)) {
                Text("Download", style = MaterialTheme.typography.labelMedium)
                HistoryChart(state.rxHistory, MaterialTheme.colorScheme.primary, rxMax, Modifier.fillMaxWidth().height(80.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Upload", style = MaterialTheme.typography.labelMedium)
                HistoryChart(state.txHistory, MaterialTheme.colorScheme.secondary, txMax, Modifier.fillMaxWidth().height(80.dp))
            }
        }
    }
}

@Composable
private fun SurfaceCard(
    title: String,
    value: String,
    content: @Composable () -> Unit,
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
