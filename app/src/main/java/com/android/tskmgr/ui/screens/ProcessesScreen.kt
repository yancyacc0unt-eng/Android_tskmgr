package com.android.tskmgr.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.tskmgr.data.ProcessInfo
import com.android.tskmgr.data.SystemMetrics
import com.android.tskmgr.ui.ProcessViewModel


@Composable
fun ProcessesScreen(viewModel: ProcessViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.snackbar) {
        val msg = state.snackbar ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeSnackbar()
    }

    Box(Modifier.fillMaxSize()) {
        if (state.loading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn {
                item {
                    ProcessHeader()
                }
                items(state.processes, key = { "${it.pid}-${it.processName}" }) { process ->
                    ProcessRow(process = process, onKill = { viewModel.killProcess(process) })
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun ProcessHeader() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Name", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("PID", Modifier.width(56.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Memory", Modifier.width(88.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(40.dp))
    }
}

@Composable
private fun ProcessRow(process: ProcessInfo, onKill: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(process, Modifier.size(36.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(process.appLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = process.processName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(process.pid.toString(), Modifier.width(56.dp), style = MaterialTheme.typography.bodySmall)
        Text(
            text = SystemMetrics.formatBytes(process.memoryKb * 1024),
            Modifier.width(88.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onKill),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Stop ${process.appLabel}",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(7.dp),
            )
        }
    }
}

@Composable
private fun AppIcon(process: ProcessInfo, modifier: Modifier = Modifier) {
    val icon = process.appIcon
    if (icon != null) {
        val bmp = remember(process.appIcon) { icon.toBitmap(96, 96).asImageBitmap() }
        Image(bmp, contentDescription = null, modifier = modifier)
    } else {
        Surface(modifier = modifier, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {}
    }
}
